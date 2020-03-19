package com.ljz.flashsales.service;

import com.ljz.flashsales.model.dto.MailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.Objects;


@Service
@EnableAsync
public class MailService {
    private static Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Environment env;

    @Async
    public void sendSimpleEmail(final MailDto dto){
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(env.getProperty("mail.send.from"));
            message.setTo(dto.getTos());
            message.setSubject(dto.getSubject());
            message.setText(dto.getContent());
            mailSender.send(message);
            log.info("发送简单文本文件");
        }catch (Exception e){
            log.error("发送简单文本文件-发生异常：",e.fillInStackTrace());
        }
    }

    /**
     * 发送HTML样式邮件
     */
    @Async
    public void sendHTMLMail(final MailDto dto){
        try{
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true,"utf-8");
            helper.setFrom(Objects.requireNonNull(env.getProperty("mail.send.from")));
            helper.setSubject(dto.getSubject());
            helper.setTo(dto.getTos());
            helper.setText(dto.getContent(),true);

            mailSender.send(mimeMessage);
            log.info("发送HTML样式邮件-发送成功");
        }catch (Exception e){
            log.error("发送HTML样式邮件-发送异常：",e.fillInStackTrace());
        }
    }
}
