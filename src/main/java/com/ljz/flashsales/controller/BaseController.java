package com.ljz.flashsales.controller;

import com.ljz.flashsales.enums.StatusCode;
import com.ljz.flashsales.response.BaseResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/base")
public class BaseController {
    private static final Logger log= LoggerFactory.getLogger(BaseController.class);

    /**
     * 跳转页面
     */
    @GetMapping("/welcome")
    public String welcome(String name, ModelMap modelMap){
        if (StringUtils.isBlank(name)){
            name="这是welcome!";
        }
        modelMap.put("name",name);
        return "welcome";
    }

    /**
     * 前端发起请求获取数据
     */
    @RequestMapping(value = "/data",method = RequestMethod.GET)
    @ResponseBody
    public String data(String name){
        if (StringUtils.isBlank(name)){
            name="这是welcome!";
        }
        return name;
    }

    /**
     * 标准请求-响应数据格式
     */
    @RequestMapping(value = "/response",method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse response(String name){
        BaseResponse response=new BaseResponse(StatusCode.Success);
        if (StringUtils.isBlank(name)){
            name="这是welcome!";
        }
        response.setData(name);
        return response;
    }

    @RequestMapping(value = "/error",method = RequestMethod.GET)
    public String error(){
        return "error";
    }
}
