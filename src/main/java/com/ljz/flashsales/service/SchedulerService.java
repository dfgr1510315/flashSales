package com.ljz.flashsales.service;

import com.ljz.flashsales.dao.ItemKillSuccessMapper;
import com.ljz.flashsales.model.entity.ItemKillSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private Environment env;
    /**
     * 定时获取status=0的订单并判断是否超过TTL,然后进行失效
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void schedulerExpireOrders(){
        log.info("定时获取status=0的订单并判断是否超过TTL,然后进行失效");
        try {
            List<ItemKillSuccess> list = itemKillSuccessMapper.selectExpireOrders();
            if (list!=null&&!list.isEmpty()){
                list.forEach(itemKillSuccess -> {
                    if (itemKillSuccess!=null&&itemKillSuccess.getDiffTime()>env.getProperty("scheduler.expire.orders.time",Integer.class)){
                        itemKillSuccessMapper.expireOrder(itemKillSuccess.getCode());
                    }
                });
            }
        }catch (Exception e){
            log.error("定时获取status=0的订单并判断是否超过TTL,然后进行失效-发生异常：",e.fillInStackTrace());
        }
    }
}
