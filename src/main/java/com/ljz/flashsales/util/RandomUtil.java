package com.ljz.flashsales.util;

import org.apache.shiro.crypto.hash.Md5Hash;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机数生成util
 **/
public class RandomUtil {

    private static final SimpleDateFormat dateFormatOne=new SimpleDateFormat("yyyyMMddHHmmssSS");

    private static final ThreadLocalRandom random=ThreadLocalRandom.current();

    /**
     * 生成订单编号-方式一
     */
    public static String generateOrderCode(){
        //TODO:时间戳+N为随机数流水号
        return dateFormatOne.format(DateTime.now().toDate()) + generateNumber(4);
    }

    //N为随机数流水号
    public static String generateNumber(final int num){
        StringBuilder sb=new StringBuilder();
        for (int i=1;i<=num;i++){
            sb.append(random.nextInt(9));
        }
        return sb.toString();
    }


/*    public static void main(String[] args) {
        String salt="ljz";
        String password="123456";
        System.out.println(new Md5Hash(password,salt));
    }*/
}