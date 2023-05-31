package org.pw.l2cache.service;

import lombok.extern.slf4j.Slf4j;
import org.pw.l2cache.po.User;
import org.pw.redisCaffeineCache.support.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 *
 * @author @yangf.
 */
@Slf4j
@Service
public class UserServiceImpl {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    static User user = new User();
    //查询时存入缓存
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_5MINS, key = "'user'+#id")
    public User getUser(Integer id) {
        log.info("new user");
        user.setId(id);
        user.setName("初始值" + sdf.format(new Date()));
        return user;
    }
    //更新方法，更新缓存
    @CachePut(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_5MINS, key = "'user'+#id")
    public User updateUser(Integer id, String name) {
        user.setId(id);
        user.setName(name + sdf.format(new Date()));
        return user;
    }
    //删除时废弃缓存
    @CacheEvict(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_5MINS, key = "'user'+#id")
    public User delete(Integer id) {
        user.setId(id);
        user.setName(null);
        return user;
    }
}
