package com.ljz.flashsales.service.Impl;

import com.ljz.flashsales.dao.ItemKillMapper;
import com.ljz.flashsales.dao.ItemKillSuccessMapper;
import com.ljz.flashsales.enums.SysConstant;
import com.ljz.flashsales.model.entity.ItemKill;
import com.ljz.flashsales.model.entity.ItemKillSuccess;
import com.ljz.flashsales.service.IItemService;
import com.ljz.flashsales.service.IKillService;
import com.ljz.flashsales.service.RabbitSenderService;
import com.ljz.flashsales.util.SnowFlake;
import org.joda.time.DateTime;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class KillService implements IKillService {

    private SnowFlake snowFlake = new SnowFlake(2, 3);

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private ItemKillMapper itemKillMapper;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private IItemService itemService;

    private static ConcurrentHashMap<Integer,Boolean> isSalesOut = new ConcurrentHashMap<>();

    /**
     * 通用的方法-记录用户秒杀成功后生成的订单-并进行异步邮件消息的通知
     */
    private void commonRecordKillSuccessInfo(ItemKill kill, Integer userId, boolean isTest) {
        ItemKillSuccess entity = new ItemKillSuccess();  //生成订单
        String orderNo = String.valueOf(snowFlake.nextId());
        entity.setCode(orderNo);
        entity.setCreateTime(DateTime.now().toDate());
        entity.setItemId(kill.getItem_id());
        entity.setKillId(kill.getId());
        entity.setStatus(SysConstant.OrderStatus.SuccessNotPayed.getCode().byteValue());
        entity.setUserId(userId.toString());
        if (itemKillSuccessMapper.insertSelective(entity) > 0) {  //插入生成的订单
            //TODO:进行异步邮件消息的通知=rabbitmq+mail
            if (!isTest) rabbitSenderService.sendKillSuccessEmailMsg(orderNo);
            //TODO:入死信队列，用于 “失效” 超过指定的TTL时间时仍然未支付的订单
            rabbitSenderService.sendKillSuccessOrderExpireMsg(orderNo);
        }else {
            stringRedisTemplate.opsForValue().set(kill.getId() + "_" + userId,null);
        }
    }

    @PostConstruct
    public void initStock() {
        try {
            List<ItemKill> list = itemService.getKillItems();
            for (ItemKill item : list) {
                if (item.getCanKill() == 1) {
                    stringRedisTemplate.opsForValue().set("itemTotal" + item.getId(), item.getTotal() + "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 商品抢购秒杀核心业务逻辑处理-redisson分布式锁
     */
    @Override
    public Boolean killItemV4(Integer killId, Integer userId, boolean isTest) throws Exception {

        if (isSalesOut.get(killId)!=null) throw new Exception("当前商品已售罄!");

        Long stock = stringRedisTemplate.opsForValue().decrement("itemTotal" + killId);
        if (stock < 0) {
            isSalesOut.put(killId,true);
            stringRedisTemplate.opsForValue().increment("itemTotal" + killId);
            throw new Exception("当前商品已售罄!");
        }

        //if (stringRedisTemplate.opsForValue().get(killId + "_" + userId) == null) { //查询此用户是否已经抢购过
            //stringRedisTemplate.opsForValue().set(killId + "_" + userId, "1", 30, TimeUnit.MINUTES);
        if (stringRedisTemplate.opsForValue().decrement(killId + "_" + userId) == -1) { //查询此用户是否已经抢购过
            //缓存中获取商品详情
            ItemKill itemKill = (ItemKill) redisTemplate.opsForValue().get("itemKill" + killId);
            if (itemKill == null) {
                //若缓存中没有则去数据库中获取并加入缓存
                itemKill = itemKillMapper.selectByIdV2(killId); //获取商品详情
                redisTemplate.opsForValue().set("itemKill" + killId, itemKill);
            }
            //TODO：库存减一
            if (itemKillMapper.updateKillItemV2(killId) > 0) { //库存减一是原子操作，可以避免超卖现象
                commonRecordKillSuccessInfo(itemKill, userId, isTest);
            } else {
                //isSalesOut.remove(killId);
                //stringRedisTemplate.opsForValue().increment("itemTotal" + killId);
                throw new Exception("当前商品已售罄!");
            }
        } else {
            stringRedisTemplate.opsForValue().increment("itemTotal" + killId);
            throw new Exception("您已经抢购过该商品了!");
        }
        return true;
    }

    /**
     * 商品抢购秒杀核心业务逻辑处理
     */
    @Override
    public Boolean killItem(Integer killId, Integer userId) throws Exception {
        boolean result = false;
        if (itemKillSuccessMapper.countByKillUserId(killId, userId) <= 0) { //查询此用户是否已经抢购过
            ItemKill itemKill = itemKillMapper.selectById(killId);
            if (itemKill != null && itemKill.getCanKill() == 1) {
                //TODO：库存减一
                if (itemKillMapper.updateKillItem(killId) > 0) {
                    commonRecordKillSuccessInfo(itemKill, userId, false);
                    result = true;
                }
            }
        } else {
            throw new Exception("您已经抢购过该商品了!");
        }
        return result;
    }

    /**
     * 商品抢购秒杀核心业务逻辑处理-对mysql加判断等条件进行优化
     */
    @Override
    public Boolean killItemV2(Integer killId, Integer userId) throws Exception {
        boolean result = false;
        if (itemKillSuccessMapper.countByKillUserId(killId, userId) <= 0) { //查询此用户是否已经抢购过
            ItemKill itemKill = itemKillMapper.selectByIdV2(killId);
            if (itemKill != null && itemKill.getCanKill() == 1) {
                //TODO：库存减一
                if (itemKillMapper.updateKillItemV2(killId) > 0) {
                    commonRecordKillSuccessInfo(itemKill, userId, false);
                    result = true;
                }
            }
        } else {
            throw new Exception("您已经抢购过该商品了!");
        }
        return result;
    }

    /**
     * 商品抢购秒杀核心业务逻辑处理-redis分布式锁
     */
    @Override
    public Boolean killItemV3(Integer killId, Integer userId) throws Exception {
        boolean result = false;
        if (itemKillSuccessMapper.countByKillUserId(killId, userId) <= 0) { //查询此用户是否已经抢购过
            //TODO:借助Redis的原子操作实现分布式锁
            ValueOperations valueOperations = stringRedisTemplate.opsForValue();
            final String key = String.valueOf(killId) + userId + "-RedisLock";
            final String value = String.valueOf(snowFlake.nextId());
            boolean cacheRes = valueOperations.setIfAbsent(key, value);
            if (cacheRes) {
                stringRedisTemplate.expire(key, 30, TimeUnit.SECONDS);
                try {
                    ItemKill itemKill = itemKillMapper.selectByIdV2(killId);
                    if (itemKill != null && itemKill.getCanKill() == 1) {
                        //TODO：库存减一
                        if (itemKillMapper.updateKillItemV2(killId) > 0) {
                            commonRecordKillSuccessInfo(itemKill, userId, false);
                            result = true;
                        }
                    }
                } catch (Exception e) {
                    throw new Exception("redis分布式锁-还没到抢购时间、已过了抢购时间或已经抢购完了!");
                } finally {
                    if (value.equals(Objects.requireNonNull(valueOperations.get(key)).toString()))
                        stringRedisTemplate.delete(key);
                }
            }
        } else {
            throw new Exception("您已经抢购过该商品了!");
        }
        return result;
    }


    @Override
    public Boolean killItemV5(Integer killId, Integer userId) throws Exception {

 /*       boolean result = false;
        //final String lockKey = String.valueOf(killId) + userId + "-RedissonLock";
        //RLock lock = redissonClient.getLock(lockKey);
        //try {
        //if (lock.tryLock(10,TimeUnit.SECONDS)){
        //System.out.println("isKilled:"+isKilled);itemKillSuccessMapper.countByKillUserId(killId,userId)<=0
        if (stringRedisTemplate.opsForValue().get(killId+"_"+userId)==null
                &&itemKillSuccessMapper.countByKillUserId(killId,userId)<=0){ //查询此用户是否已经抢购过
            //缓存中获取商品详情
            ItemKill itemKill = (ItemKill)redisTemplate.opsForValue().get("itemKill"+killId);
            //缓存中获取商品剩余数量
            Integer itemTotal;
            if (itemKill==null){
                //若缓存中没有则去数据库中获取并加入缓存
                itemKill = itemKillMapper.selectByIdV2(killId); //获取商品详情
                redisTemplate.opsForValue().set("itemKill"+killId,itemKill);
                itemTotal = itemKill.getTotal();
                stringRedisTemplate.opsForValue().set("itemTotal" + killId,itemTotal.toString());
            }else{
                itemTotal = Integer.valueOf(Objects.requireNonNull(stringRedisTemplate.opsForValue().get("itemTotal" + killId)));
            }

            if (itemTotal>0){
                //TODO：库存减一
                if (itemKillMapper.updateKillItemV2(killId)>0){ //库存减一是原子操作，可以避免超卖现象
                    stringRedisTemplate.opsForValue().decrement("itemTotal" + killId);
                    //redisTemplate.opsForValue().set("itemKill"+killId, itemKill);
                    commonRecordKillSuccessInfo(itemKill,userId,isTest);
                    result=true;
                }else{
                    throw new Exception("当前商品已售罄!");
                }
            }
        }else {
            throw new Exception("redisson分布式锁-您已经抢购过该商品了!");
        }
        *//*    }
        }finally {
            lock.unlock();
        }*//*
        return result;*/
        return null;
    }
}
