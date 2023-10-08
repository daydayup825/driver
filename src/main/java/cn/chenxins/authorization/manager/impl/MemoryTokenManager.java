package cn.chenxins.authorization.manager.impl;


import cn.chenxins.authorization.manager.TokenManager;
import cn.chenxins.exception.TokenException;
import cn.chenxins.utils.ConstConfig;
import cn.chenxins.utils.DesUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 通过Redis存储和验证token的实现类
 * @see TokenManager
 * @author ScienJus
 * @date 2015/7/31.
 */
@Component("memoryTokenManager")
public class MemoryTokenManager implements TokenManager {


    // 创建一个带有过期时间的缓存
    private Cache<String, String> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(100, TimeUnit.MINUTES) // 设置写入后的过期时间
            .build();

    public String createToken(String uid) throws TokenException {
        //使用uuid作为源token,一步部分
        String preToken = UUID.randomUUID().toString().replace("-", "");

        String key=preToken+"."+DesUtils.md5("access_token");

        //存储到redis并设置过期时间
        cache.put(key,uid);

        return key;
    }

    /**
     * 创建一个RefreshToken
     */
    public String createReToken(String accessToken) throws TokenException {
        //使用uuid作为源token,一步部分
   /*     String preToken = UUID.randomUUID().toString().replace("-", "");

        String key=preToken+"."+DesUtils.md5("refresh_token");


        //存储到redis并设置过期时间
        cache.put(key,accessToken);*/
        return "";
    }


    public Integer checkToken(String authentication)throws TokenException {

        if (authentication == null || "".equals(authentication.trim())) {
            return null;
        }
        String tokenValue = cache.getIfPresent(authentication);
        if (tokenValue == null || "".equals(tokenValue.trim())) {
            return null;
        } else {
            return Integer.parseInt(tokenValue);

        }

        //如果验证成功，说明此用户进行了一次有效操作，延长token的过期时间
//        redis.expire(authentication, ConstConfig.TOKEN_EXPIRES_HOUR*3600);
    }

    public Integer refreshCheckToken(String refreshToken) throws TokenException {
        String accessToken = cache.getIfPresent(refreshToken);
        if (accessToken == null || "".equals(accessToken.trim()) ) {
            return null;
        } else {
            String uid=cache.getIfPresent(accessToken);
            if (uid == null || "".equals(uid.trim()) ) {
                return null;
            }
            deleteToken(accessToken);//清除原来的accessToken
            return Integer.parseInt(uid);

        }
    }


    public void deleteToken(String tokenKey) {
//       redisTemplate.delete(userId);
        cache.invalidate(tokenKey);
    }
}
