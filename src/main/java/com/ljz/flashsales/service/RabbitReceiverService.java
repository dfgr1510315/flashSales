package com.ljz.flashsales.service;

import com.ljz.flashsales.dao.ItemKillSuccessMapper;
import com.ljz.flashsales.model.dto.KillSuccessUserInfo;
import com.ljz.flashsales.model.dto.MailDto;
import com.ljz.flashsales.model.entity.ItemKillSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * RabbitMQ接收消息服务
 */
@Service
public class RabbitReceiverService {

    private static final Logger log =  LoggerFactory.getLogger(RabbitReceiverService.class);

    @Autowired
    private MailService mailService;

    @Autowired
    private Environment env;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    /**
     * 秒杀接收消息
     */
    @RabbitListener(queues = {"${mq.kill.item.success.email.queue}"},containerFactory = "singleListenerContainer")
    public void consumeEmailMsg(KillSuccessUserInfo info){
        try{
            log.info("秒杀成功异步发送邮件通知消息-开始接收消息：{}",info);
            final String content = String.format(Objects.requireNonNull(env.getProperty("mail.kill.item.success.content")),
                    info.getItemName(),info.getCode());
            //TODO:发送邮件
            MailDto dto = new MailDto(env.getProperty("mail.kill.item.success.subject"),
                    content, new String[]{info.getEmail()});
            mailService.sendHTMLMail(dto);
        }catch (Exception e){
            log.error("秒杀成功异步发送邮件通知消息-接收消息发送异常：",e.fillInStackTrace());
        }
    }

    /**
     * 消费并处理过期订单
     */
    @RabbitListener(queues = {"${mq.kill.item.success.kill.dead.real.queue}"},containerFactory = "singleListenerContainer")
    public void consumeExpireOrder(KillSuccessUserInfo info){
        try{
            log.info("消费并处理过期订单：{}",info);
            if (info!=null){
                ItemKillSuccess itemKillSuccess = itemKillSuccessMapper.selectByPrimaryKey(info.getCode());
                if (itemKillSuccess!=null&&itemKillSuccess.getStatus()==0){
                    itemKillSuccessMapper.expireOrder(info.getCode());
                }
            }
        }catch (Exception e){
            log.error("消费并处理过期订单-发生异常：{}",info,e.fillInStackTrace());
        }
    }



}
