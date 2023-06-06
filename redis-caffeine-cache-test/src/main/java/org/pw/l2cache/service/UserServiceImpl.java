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
 * @author @yangf.
 */
@Slf4j
@Service
public class UserServiceImpl {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
    static User user = new User();

    //查询时存入缓存，sync=true:解决缓存击穿问题（本地查加锁，避免高并发下获取不到缓存都去执行实际方法）
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_5MINS, key = "'user'+#id", sync = true)
    public User getUserSync(Integer id) {
        log.info("new user");
        user.setId(id);
        user.setName("初始值" + sdf.format(new Date()));
        return user;
    }

    //查询时存入缓存
    @Deprecated
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

    // 测试不存在的数据
    @Cacheable(
      cacheManager = "L2_CacheManager",// 只配置一个缓存组件时不需要显示指定此参数
      cacheNames = CacheNames.CACHE_5MINS,
      key = "'user'+#id",
      sync = true //避免缓存击穿
    )
    public User getUserWithoutCreate(Integer id) {
        log.info("service getUserWithoutCreate", id);
        return null;
    }
}
