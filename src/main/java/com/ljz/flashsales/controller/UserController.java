package com.ljz.flashsales.controller;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private Environment env;
    /**
     * 跳转到登录界面
     */
    @RequestMapping(value = {"/to/login","/unauth"})
    public String toLogin(){
        log.info("跳转到登录页面");
        return "login";
    }

    @RequestMapping(value = "/login",method = RequestMethod.POST)
    public String login(@RequestParam String userName, @RequestParam String password, ModelMap modelMap){
        String errorMeg = "";
        try {
            if (!SecurityUtils.getSubject().isAuthenticated()) {//查看当前用户是否已经认证过
                UsernamePasswordToken token = new UsernamePasswordToken(userName, new Md5Hash(password,env.getProperty("shiro.encrypt.password.salt")).toString());
                SecurityUtils.getSubject().login(token);
            }
        }catch (UnknownAccountException | DisabledAccountException | IncorrectCredentialsException e){
            errorMeg=e.getMessage();
            modelMap.addAttribute("username",userName);
        }
        if (StringUtils.isBlank(errorMeg)){
            return "redirect:/item/index";
        }else {
            modelMap.addAttribute("errorMsg",errorMeg);
            return "login";
        }
    }

    @RequestMapping(value = {"/logout"})
    public String logout(){
        SecurityUtils.getSubject().logout();
        return "login";
    }
}
