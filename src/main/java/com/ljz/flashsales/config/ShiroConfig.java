package com.ljz.flashsales.config;

import com.ljz.flashsales.service.CustomRealm;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {
    @Bean
    public CustomRealm customRealm(){
        return new CustomRealm();
    }

    @Bean
    public SecurityManager securityManager(){
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(customRealm());
        return securityManager;
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(){
        ShiroFilterFactoryBean bean = new ShiroFilterFactoryBean();
        bean.setSecurityManager(securityManager());
        bean.setLoginUrl("/to/login");
        bean.setUnauthorizedUrl("/unauth");
        Map<String, String> filterChainDefinitionMap = new HashMap<>();
        filterChainDefinitionMap.put("/to/login","anon");//访问toLogin页面时直接放行
        filterChainDefinitionMap.put("/kill/execute/lock","anon");
        filterChainDefinitionMap.put("/**","anon");
        filterChainDefinitionMap.put("/item/detail/*","authc");
        filterChainDefinitionMap.put("/kill/execute/","authc");
        bean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return bean;
    }
}
