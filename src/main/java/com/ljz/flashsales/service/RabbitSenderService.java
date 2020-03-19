package com.ljz.flashsales.service;

import com.ljz.flashsales.dao.ItemKillSuccessMapper;
import com.ljz.flashsales.model.dto.KillSuccessUserInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;


/**
 * RabbitMQ发送消息服务
 */
@Service
public class RabbitSenderService {

    private static final Logger log =  LoggerFactory.getLogger(RabbitSenderService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    public void sendKillSuccessEmailMsg(String orderNo){
        log.info("秒杀成功异步发送邮件通知消息-准备发送消息:{}",orderNo);
        try{
            if (StringUtils.isNotBlank(orderNo)){
                KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderNo); //根据订单号获取订单信息（包括用户信息在内）
                if (info!=null){
                    //TODO：rabbitMQ发送消息逻辑
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange(environment.getProperty("mq.kill.item.success.email.exchange"));
                    rabbitTemplate.setRoutingKey(environment.getProperty("mq.kill.item.success.email.routing.key"));
                    //rabbitTemplate.convertAndSend(MessageBuilder.withBody(orderNo.getBytes(StandardCharsets.UTF_8)).build());
                    //TODO:将info充当消息发送至队列
                    rabbitTemplate.convertAndSend(info, message -> {
                        MessageProperties messageProperties = message.getMessageProperties();
                        //持久化保证消息可靠性
                        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME,KillSuccessUserInfo.class);
                        return message;
                    });
                }
            }
        }catch (Exception e){
            log.error("秒杀成功异步发送邮件通知消息-发送异常:{}",orderNo,e.fillInStackTrace());
        }
    }

    /**
     * 秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效超时未支付的订单
     */
    public void sendKillSuccessOrderExpireMsg(final String orderCode){
        log.info("秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效超时未支付的订单:{}",orderCode);
        try {
            if(StringUtils.isNotBlank(orderCode)){
                KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderCode);
                if (info!=null){
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange(environment.getProperty("mq.kill.item.success.kill.dead.prod.exchange"));
                    rabbitTemplate.setRoutingKey(environment.getProperty("mq.kill.item.success.kill.dead.prod.routing.key"));
                    rabbitTemplate.convertAndSend(info,message -> {
                        MessageProperties messageProperties = message.getMessageProperties();
                        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME,KillSuccessUserInfo.class);
                        //TODO:动态设置TTL
                        messageProperties.setExpiration(environment.getProperty("mq.kill.item.success.kill.expire"));
                        return message;
                    });
                }
            }
        }catch (Exception e){
            log.error("秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效超时未支付的订单-发生异常：{}",orderCode,e.fillInStackTrace());
        }

    }
}
