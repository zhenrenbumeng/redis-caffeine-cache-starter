package org.pw.l2cache.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.pw.l2cache.po.User;
import org.pw.l2cache.service.UserServiceImpl;
import org.pw.redisCaffeineCache.support.CacheNames;
import org.pw.redisCaffeineCache.support.RedisCaffeineCache;
import org.pw.redisCaffeineCache.support.RedisCaffeineCacheManager;
import org.springframework.cache.Cache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * redis-caffeine-cache 测试类
 *
 * @author @yangf.
 */
@RestController
@Slf4j
public class Controller {
    @Resource
    UserServiceImpl userService;
    @Resource
    RedisCaffeineCacheManager redisCaffeineCacheManager;

    /**
     * 测试缓存
     *
     * @param id
     * @return
     */
    @GetMapping("/test")
    public User test(Integer id) {
        User user;
        /**
         * 根据id初始化一个用户
         */
        user = userService.getUser(id);
        log.info("getUser {} {}", id, JSONObject.toJSONString(user));
        /**
         * 再次根据id查询，走本地缓存
         */
        user = userService.getUser(id);
        log.info("getUser {} {}", id, JSONObject.toJSONString(user));
        /**
         * 更新缓存
         */
        user = userService.updateUser(1, "张三");
        log.info("user after update {} {}", id, JSONObject.toJSONString(user));

        /**
         * 必须等待收到redis清除缓存消息，清空本地缓存后，才能取到最新值“张三”
         */
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /**
         * 再次根据id查询，走本地缓存
         */
        user = userService.getUser(id);
        log.info("getUser {} {}", id, JSONObject.toJSONString(user));

        return user;
    }

    @GetMapping("/get")
    public User get(Integer id) {
        User user = userService.getUser(id);
        log.info("getUser {} {}", id, JSONObject.toJSONString(user));
        return user;
    }

    @GetMapping("/clearAllCache")
    public void clearAllCache() {
        //先清空redis缓存
        redisCaffeineCacheManager.clearAllCache();
    }

    @GetMapping("/update")
    public User update(Integer id, String name) {
        User user = userService.updateUser(id, name);
        log.info("user after update {} {}", id, JSONObject.toJSONString(user));
        return user;
    }

    /**
     * 删除缓存
     *
     * @param id
     */
    @GetMapping("/evict")
    public void clearAllLocal(Integer id) {
        userService.delete(id);
    }

    /**
     * 显示所有cache
     *
     * @param cacheName
     * @return
     */
    @GetMapping("allCaches")
    public String showCaches(String cacheName) {
        String s = redisCaffeineCacheManager.allCaches(cacheName);
        log.info("allCaches cacheName:{} {}", cacheName, JSONObject.toJSONString(s));
        return s;
    }

    @GetMapping("/getNull")
    public User getNull(Integer notExistId) {
        User user = userService.getUserWithoutCreate(notExistId);
        log.info("getNull user:{}", JSONObject.toJSONString(user));
        return user;
    }
}
