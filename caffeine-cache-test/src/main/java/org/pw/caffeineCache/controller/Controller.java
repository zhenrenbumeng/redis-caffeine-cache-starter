package org.pw.caffeineCache.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.pw.caffeineCache.service.UserServiceImpl;
import org.pw.caffeineCache.po.User;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

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
    @GetMapping("/user")
    public User getUser(Integer id) {
        return userService.getUser(id);
    }
    @Resource
    CacheManager commonCacheManager;
    @GetMapping("/get")
    public User get(Integer id) {
        User user = userService.getUser(id);
        log.info("getUser {} {}", id, JSONObject.toJSONString(user));
        return user;
    }
    @GetMapping("/update")
    public User update(Integer id, String name) {
        User user = userService.updateUser(id, name);
        log.info("user after update {} {}", id, JSONObject.toJSONString(user));
        return user;
    }
    @GetMapping("/evict")
    public void clearAllLocal(Integer id) {
        userService.delete(id);
    }
    @GetMapping("/allCaches")
    public String showCaches(String cacheName) {
        if (cacheName != null) {
            Cache cache = commonCacheManager.getCache(cacheName);
            if (cache != null) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                ConcurrentMap<@NonNull Object, @NonNull Object> objectObjectConcurrentMap = nativeCache.asMap();
                JSONArray jsonArray = new JSONArray();
                for (Object o : objectObjectConcurrentMap.keySet()) {
                    JSONObject object = new JSONObject();
                    object.put(o.toString(), objectObjectConcurrentMap.get(o));
                    jsonArray.add(object);
                }
                log.info("caches cacheName:{} {}", cacheName, jsonArray.toJSONString());
                return jsonArray.toJSONString();
            }
            return JSONObject.toJSONString(cache);
        }
        Collection<String> cacheNames = commonCacheManager.getCacheNames();
        JSONArray jsonArray = new JSONArray();
        if (!CollectionUtils.isEmpty(cacheNames)) {
            for (String name : cacheNames) {
                JSONObject object = new JSONObject();
                Cache cache = commonCacheManager.getCache(name);

                if (cache != null) {
                    CaffeineCache caffeineCache = (CaffeineCache) cache;
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
                    ConcurrentMap<@NonNull Object, @NonNull Object> objectObjectConcurrentMap = nativeCache.asMap();
                    JSONArray jsonArray2 = new JSONArray();
                    for (Object o : objectObjectConcurrentMap.keySet()) {
                        JSONObject object2 = new JSONObject();
                        object2.put(o.toString(), objectObjectConcurrentMap.get(o));
                        jsonArray2.add(object);
                    }
                    object.put(name, jsonArray2);
                    return jsonArray.toJSONString();
                }
                jsonArray.add(object);
            }
            log.info("caches all: {}", jsonArray.toJSONString());
        }
        return jsonArray.toJSONString();
    }
    @GetMapping("/clearAllCache")
    public void clearAllCache() {
        Collection<String> cacheNames = commonCacheManager.getCacheNames();
        if (!CollectionUtils.isEmpty(cacheNames)) {
            for (String name : cacheNames) {
                commonCacheManager.getCache(name).clear();
                log.info("clear cache cacheName:{}", name);
            }
        }
    }
}
