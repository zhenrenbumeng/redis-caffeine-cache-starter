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
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    //查询时存入缓存，sync=true:解决缓存击穿问题（本地查加锁，避免高并发下获取不到缓存都去执行实际方法）
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_1MIN, key = "'user'+#id", sync = true)
    public User getUserSync(Integer id) {
        User user = new User();
        user.setId(id);
        user.setName(id + "_" + sdf.format(new Date()));
        log.info("getUser user:{}", user);
        return user;
    }

    //region NullValue问题测试
    /**
     * sync为false，执行RedisCaffeineCache.lookup方法，将返回NullValue,出现ClassCastException
     */
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_5MINS, key = "'user'+#id")
    public User getUserNull(Integer id) {
        return null;
    }

    /**
     * 避免NullValue正确处理方式1：sync=true,调用RedisCaffeineCache.get方法
     * sync=true:解决缓存击穿问题（本地查加锁，避免高并发下获取不到缓存都去执行实际方法）
     */
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_5MINS, key = "'user'+#id", sync = true)
    public User getUserNullSync(Integer id) {
        return null;
    }


    /**
     * 避免NullValue正确处理方式2: unless="#result==null"
     */
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_5MINS, key = "'user'+#id", condition = "#id!=null", unless = "#result==null")
    public User getUserNullCondition(Integer id) {
        if (id == null) {
            return null;
        }
        return new User(id, id + "_" + sdf.format(new Date()));
    }
    //endregion

    //查询时存入缓存
    @Deprecated
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_1MIN, key = "'user'+#id", sync = true)
    public User getUser(Integer id) {
        User user = new User();
        user.setId(id);
        user.setName(id + "_" + sdf.format(new Date()));
        log.info("getUser user:{}", user);
        return user;
    }

    //更新方法，更新缓存
    @CachePut(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_1MIN, key = "'user'+#id")
    public User updateUser(Integer id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name + sdf.format(new Date()));
        log.info("updateUser id:{}, name:{}", id, user.getName());
        return user;
    }

    //删除时废弃缓存
    @CacheEvict(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_1MIN, key = "'user'+#id")
    public User delete(Integer id) {
        User user = new User();
        user.setId(id);
        user.setName(null);
        return user;
    }

    // 测试不存在的数据
    @Cacheable(
            cacheManager = "L2_CacheManager",// 只配置一个缓存组件时不需要显示指定此参数
            cacheNames = CacheNames.CACHE_1MIN,
            key = "'user'+#id",
            sync = true //避免缓存击穿
    )
    public User getUserWithoutCreate(Integer id) {
        log.info("service getUserWithoutCreate {}", id);
        return null;
    }
}
