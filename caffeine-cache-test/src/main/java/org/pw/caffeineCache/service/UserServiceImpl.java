package org.pw.caffeineCache.service;

import lombok.extern.slf4j.Slf4j;
import org.pw.caffeineCache.po.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
public class UserServiceImpl {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    static User user = new User();

    //查询时存入缓存
    @Cacheable(cacheManager = "commonECManager", cacheNames = "users", key = "'user'+#id")
    public User getUser(Integer id) {
        log.info("new user");
        user.setId(id);
        user.setName("初始值" + sdf.format(new Date()));
        return user;
    }

    //更新方法，更新缓存
    @CachePut(cacheManager = "commonECManager", cacheNames = "users", key = "'user'+#id")
    public User updateUser(Integer id, String name) {
        user.setId(id);
        user.setName(name + sdf.format(new Date()));
        return user;
    }

    //删除时废弃缓存
    @CacheEvict(cacheManager = "commonECManager", cacheNames = "users", key = "'user'+#id")
    public User delete(Integer id) {
        log.info("delete user(id:{}), evict cache", id);
        user.setId(id);
        user.setName(null);
        return user;
    }

}
