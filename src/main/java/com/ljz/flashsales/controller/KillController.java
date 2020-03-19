package com.ljz.flashsales.controller;

import com.ljz.flashsales.dao.ItemKillSuccessMapper;
import com.ljz.flashsales.enums.StatusCode;
import com.ljz.flashsales.model.dto.KillDto;
import com.ljz.flashsales.model.dto.KillSuccessUserInfo;
import com.ljz.flashsales.response.BaseResponse;
import com.ljz.flashsales.service.IKillService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("kill")
public class KillController {

    private static final Logger log= LoggerFactory.getLogger(KillController.class);

    @Autowired
    private IKillService iKillService;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    /**
     * 核心业务逻辑，秒杀抢购
     */
    @RequestMapping(value = "/execute",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse execute(@RequestBody @Validated KillDto killDto, BindingResult bindingResult, HttpSession session){
        if (bindingResult.hasErrors()||killDto.getKillId()<=0){
            return new BaseResponse(StatusCode.InvalidParams);
        }
        Object uid = session.getAttribute("uid");
        if (uid==null) return new BaseResponse(StatusCode.UserNotLogin);
        try{
            if (!iKillService.killItemV4(killDto.getKillId(),(Integer) uid,false)){
                return new BaseResponse(StatusCode.Fail.getCode(),"商品已抢购完毕或者不在抢购时间段");
            }
        }catch (Exception e){
            log.info("异常信息:{}",e.getMessage());
            return new BaseResponse(StatusCode.Fail.getCode(),e.getMessage());
        }
        return new BaseResponse(StatusCode.Success);
    }

    /**
     * 核心业务逻辑，秒杀抢购-并发测试
     */
    @RequestMapping(value = "/execute/lock",method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse executeLock(@RequestBody @Validated KillDto killDto, BindingResult bindingResult){
        if (bindingResult.hasErrors()||killDto.getKillId()<=0){
            return new BaseResponse(StatusCode.InvalidParams);
        }
        try{
            /*//不加分布式锁
            if (!iKillService.killItem(killDto.getKillId(),killDto.getUserId())){
                return new BaseResponse(StatusCode.Fail.getCode(),"测试——商品已抢购完毕或者不在抢购时间段");
            }*/
            //Object uid = session.getAttribute("uid");
            //基于Redis的分布式锁进行并发控制
            if (!iKillService.killItemV4(killDto.getKillId(),killDto.getUserId(),true)){
                return new BaseResponse(StatusCode.Fail.getCode(),"基于Redis的分布式锁测试——商品已抢购完毕或者不在抢购时间段");
            }
        }catch (Exception e){
            log.info("异常信息:{}",e.getMessage());
            return new BaseResponse(StatusCode.Fail.getCode(),e.getMessage());
        }
        return new BaseResponse(StatusCode.Success);
    }

    /**
     * 订单详情
     */
    @RequestMapping(value = "/record/detail/{orderNo}",method = RequestMethod.GET)
    public String killRecordDetail(@PathVariable String orderNo, ModelMap modelMap){
        log.info("订单详情:{}",orderNo);
        if (StringUtils.isBlank(orderNo)) {
            log.info("订单详情：orderNo为空");
            return "error";
        }
        KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderNo);
        if (info==null) {
            log.info("订单详情：info为空");
            return "error";
        }
        modelMap.put("info",info);
        return "killRecord";
    }

    @RequestMapping(value = "/execute/success",method = RequestMethod.GET)
    public String executeSuccess(){
        return "executeSuccess";
    }

    @RequestMapping(value = "/execute/fail",method = RequestMethod.GET)
    public String executeFail(){
        return "executeFail";
    }


}
