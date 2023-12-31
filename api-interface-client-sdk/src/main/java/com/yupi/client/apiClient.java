package com.yupi.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.yupi.untils.SignUntils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 第三方调用客户端
 */
@Slf4j
@AllArgsConstructor
public class apiClient {

    private String accessKey;
    private String secretKey;
    private StringRedisTemplate stringRedisTemplate;
    private static final String AK= "AK-SK";

    public apiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

//    public String getNameByGet(String name){
//        HashMap<String, Object> paramMap = new HashMap<>();
//        paramMap.put("name", name);
//
//        String result = HttpUtil.get("localhost:8123/api/name/", paramMap);
//        return result;
//    }
//
//    public String getNameByPost(String name){
//        HashMap<String, Object> paramMap = new HashMap<>();
//        paramMap.put("name", name);
//
//        String result= HttpUtil.post("localhost:8123/api/name/", paramMap);
//        return result;
//    }

    @Transactional
    public Map<String,String> putHeader(String body){
        HashMap<String, String> headerKey = new HashMap<>();

        headerKey.put("accessKey",accessKey);
        headerKey.put("body",body);
        headerKey.put("timestamp",String.valueOf(System.currentTimeMillis() / 1000));
        String nonce = RandomUtil.randomNumbers(4);
        headerKey.put("nonce",nonce);
        stringRedisTemplate.opsForValue().set(AK +":" + accessKey,nonce,5L, TimeUnit.MINUTES);
        headerKey.put("sign", SignUntils.Sign(body,secretKey));
        return headerKey;
    }


    public String getNameByJson(Object object,String url){
        String json = JSONUtil.toJsonStr(object);
        HttpResponse response = HttpRequest.post(url)
                .addHeaders(putHeader(json))
                .body(json)
                .execute();
        int status = response.getStatus();
        String result = null;
        if (status != HttpStatus.HTTP_OK){
            log.info("状态码:" + status);
            throw new RuntimeException("访问失败:"+status);
        }
        result = response.body();
        log.info("调用成功");
        return result;
    }
}
