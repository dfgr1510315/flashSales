package com.ljz.flashsales.service.Impl;

import com.ljz.flashsales.dao.ItemKillMapper;
import com.ljz.flashsales.model.entity.ItemKill;
import com.ljz.flashsales.service.IItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService implements IItemService {

    private static final Logger log= LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemKillMapper itemKillMapper;


    /**
     * 获取待秒杀商品列表
     */
    @Override
    public List<ItemKill> getKillItems() {
        return itemKillMapper.selectAll();
    }

    /**
     * 获取秒杀详情
     */
    @Override
    public ItemKill getKillDetail(Integer id) throws Exception {
        ItemKill entity=itemKillMapper.selectByIdV2(id);
        if (entity==null){
            throw new Exception("获取秒杀详情-待秒杀商品记录不存在");
        }
        return entity;
    }
}



















