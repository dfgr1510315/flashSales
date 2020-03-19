package com.ljz.flashsales.service;

import com.ljz.flashsales.dao.UserMapper;
import com.ljz.flashsales.model.entity.User;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;


/**
 * 用户自定义的realm-用于shiro的认证、授权
 */
public class CustomRealm extends AuthorizingRealm {

    private static final Logger log = LoggerFactory.getLogger(CustomRealm.class);

    //测试时长30_000
    private static final Long sessionKeyTimeOut = 1800000L;

    @Autowired
    private UserMapper userMapper;



    /**
     * 授权
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {

        return null;
    }

    /**
     * 认证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        String username = token.getUsername();
        String password = String.valueOf(token.getPassword());
        log.info("当前登录的用户名={} 密码={}",username,password);
        User user = userMapper.selectByUserName(username);
        if (user==null) throw new UnknownAccountException("用户名不存在");
        log.info("当前user:{}",user);
        if (user.getIsActive().intValue()!=1)  throw new DisabledAccountException("当前用户账号被禁用");
        if (!user.getPassword().equals(password)) throw new IncorrectCredentialsException("用户密码不匹配");
        setSession("uid",user.getId());
        return new SimpleAuthenticationInfo(user.getUserName(),password,getName());
    }

    /**
     * 将key与对应的value存入shiro的session中-最终交给HTTPSession进行管理
     * 如果是分布式则交给redis-session进行管理
     */
    private void setSession(String key,Object value){
        Session session = SecurityUtils.getSubject().getSession();
        if (session!=null){
            session.setAttribute(key,value);
            session.setTimeout(sessionKeyTimeOut);
        }
    }
}
