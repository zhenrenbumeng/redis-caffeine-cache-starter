package org.pw.redisCaffeineCache.support;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.pw.redisCaffeineCache.CacheRedisCaffeineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


@Slf4j
public class RedisCaffeineCacheManager implements CacheManager {

    private final Logger logger = LoggerFactory.getLogger(RedisCaffeineCacheManager.class);

    private static ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>();

    private CacheRedisCaffeineProperties cacheRedisCaffeineProperties;

    private RedisTemplate<Object, Object> stringKeyRedisTemplate;

    private boolean dynamic = true;

    private Set<String> cacheNames;

    {
        cacheNames = new HashSet<>();
        cacheNames.add(CacheNames.CACHE_5MINS);
        cacheNames.add(CacheNames.CACHE_15MINS);
        cacheNames.add(CacheNames.CACHE_60MINS);
        cacheNames.add(CacheNames.CACHE_12HOUR);
        cacheNames.add(CacheNames.CACHE_24HOUR);
        cacheNames.add(CacheNames.CACHE_PERMANENT);
    }

    public RedisCaffeineCacheManager(CacheRedisCaffeineProperties cacheRedisCaffeineProperties,
                                     RedisTemplate<Object, Object> stringKeyRedisTemplate) {
        super();
        this.cacheRedisCaffeineProperties = cacheRedisCaffeineProperties;
        this.stringKeyRedisTemplate = stringKeyRedisTemplate;
        this.dynamic = cacheRedisCaffeineProperties.isDynamic();
    }

    //——————————————————————— 进行缓存工具 ——————————————————————

    /**
     * 清除所有进程缓存
     */
    public void clearAllCache() {
        stringKeyRedisTemplate.convertAndSend(cacheRedisCaffeineProperties.getRedis().getTopic(), new CacheMessage(null, null, MDC.get(TraceIdUtils.TRACE_ID)));
    }

    /**
     * 清除Redis以及本地缓存
     *
     * @param cacheName null则清空所有缓存
     */
    @Deprecated //clearAllCache未清空redis的问题已修正，废弃此方法
    public void clearAllCacheAndRedis(String cacheName) {
        log.info("cache---------- RedisCaffeineCacheManager clearAllCacheAndRedis");
        if (cacheName != null) {
            Cache cache = cacheMap.get(cacheName);
            if (cache == null) {
                return;
            }
            RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
            redisCaffeineCache.clear();
        } else {
            for (Cache cache : cacheMap.values()) {
                RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
                redisCaffeineCache.clear();
            }
        }
        clearAllCache();
    }

    public String allCaches(String cacheName) {
        JSONArray jsonArray = new JSONArray();

        if (cacheName != null) {
            Cache cache = cacheMap.get(cacheName);
            if (cache == null) {
                return jsonArray.toString();
            }
            RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
            ConcurrentMap<@NonNull Object, @NonNull Object> objectObjectConcurrentMap = redisCaffeineCache.getCaffeineCache().asMap();

            JSONObject obj = new JSONObject();
            obj.put(cacheName, objectObjectConcurrentMap);
            jsonArray.add(obj);
        } else {
            for (Object o : cacheMap.keySet()) {
                Cache cache = cacheMap.get(o);
                RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
                ConcurrentMap<@NonNull Object, @NonNull Object> objectObjectConcurrentMap = redisCaffeineCache.getCaffeineCache().asMap();
                JSONObject obj = new JSONObject();
                obj.put(o.toString(), objectObjectConcurrentMap);
                jsonArray.add(obj);
            }
        }
        log.info("cache---------- RedisCaffeineCacheManager allCaches cacheName:{} {}", cacheName, jsonArray.toJSONString());
        return jsonArray.toJSONString();
    }

    /**
     * 返回所有进程缓存(二级缓存)的统计信息
     * result:{"缓存名称":统计信息}
     *
     * @return
     */
    public static Map<String, CacheStats> getCacheStats() {
        if (CollectionUtils.isEmpty(cacheMap)) {
            return null;
        }

        Map<String, CacheStats> result = new LinkedHashMap<>();
        for (Cache cache : cacheMap.values()) {
            RedisCaffeineCache caffeineCache = (RedisCaffeineCache) cache;
            result.put(caffeineCache.getName(), caffeineCache.getCaffeineCache().stats());
        }
        return result;
    }

    //—————————————————————————— core —————————————————————————
    @Override
    public Cache getCache(String name) {
        Cache cache = cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        if (!dynamic && !cacheNames.contains(name)) {
            return null;
        }

        cache = new RedisCaffeineCache(name, stringKeyRedisTemplate, caffeineCache(name), cacheRedisCaffeineProperties);
        Cache oldCache = cacheMap.putIfAbsent(name, cache);
        logger.info("cache---------- RedisCaffeineCacheManager create cache instance, the cache name is : {}", name);
        return oldCache == null ? cache : oldCache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }

    public void evict(String cacheName, Object key) {
        logger.info("cache---------- RedisCaffeineCacheManager evict {} {}", cacheName, key);
        //cacheName为null 清除所有进程缓存
        if (cacheName == null) {
            log.info("cache---------- RedisCaffeineCacheManager cacheName is null, 清除所有本地缓存");
            // cacheMap = new ConcurrentHashMap<>();
            // 保持map基本结构存在，以便清除redis
            for (Cache value : cacheMap.values()) {
                RedisCaffeineCache cache = (RedisCaffeineCache) value;
                cache.clear();
            }
            return;
        }

        Cache cache = cacheMap.get(cacheName);
        if (cache == null) {
            return;
        }

        RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
        // redisCaffeineCache.clearLocal(key);
        redisCaffeineCache.evict(key);
    }

    public void clearLocal(String cacheName, Object key) {
        //cacheName为null 清除所有进程缓存
        if (cacheName == null) {
            log.info("cache---------- RedisCaffeineCacheManager cacheName is null, 清除所有本地缓存");
            // cacheMap = new ConcurrentHashMap<>();
            for (Cache value : cacheMap.values()) {
                RedisCaffeineCache cache = (RedisCaffeineCache) value;
                cache.clear();
            }
            return;
        }

        Cache cache = cacheMap.get(cacheName);
        if (cache == null) {
            return;
        }

        RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
        redisCaffeineCache.clearLocal(key);
    }

    /**
     * 当前应用场景是判断CacheMessage是否在本地缓存中
     *
     * @param cacheName
     * @param key
     * @return
     */
    public Object getLocal(String cacheName, Object key) {
        if (cacheName == null) {
            return null;
        }

        Cache cache = cacheMap.get(cacheName);
        if (cache == null) {
            return null;
        }

        RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
        Object obj = redisCaffeineCache.getCaffeineCache().getIfPresent(key);
        return obj;
    }

    /**
     * 实例化本地一级缓存
     *
     * @param name
     * @return
     */
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache(String name) {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
        CacheRedisCaffeineProperties.CacheDefault cacheConfig;
        switch (name) {
            case CacheNames.CACHE_5MINS:
                cacheConfig = cacheRedisCaffeineProperties.getCache5m();
                break;
            case CacheNames.CACHE_15MINS:
                cacheConfig = cacheRedisCaffeineProperties.getCache15m();
                break;
            case CacheNames.CACHE_60MINS:
                cacheConfig = cacheRedisCaffeineProperties.getCache60m();
                break;
            case CacheNames.CACHE_12HOUR:
                cacheConfig = cacheRedisCaffeineProperties.getCache12h();
                break;
            case CacheNames.CACHE_24HOUR:
                cacheConfig = cacheRedisCaffeineProperties.getCache24h();
                break;
            case CacheNames.CACHE_PERMANENT:
                cacheConfig = cacheRedisCaffeineProperties.getCachePermanent();
                break;
            default:
                cacheConfig = cacheRedisCaffeineProperties.getCacheDefault();
        }
        long expireAfterAccess = cacheConfig.getExpireAfterAccess();
        long expireAfterWrite = cacheConfig.getExpireAfterWrite();
        int initialCapacity = cacheConfig.getInitialCapacity();
        long maximumSize = cacheConfig.getMaximumSize();
        long refreshAfterWrite = cacheConfig.getRefreshAfterWrite();

        log.info("cache---------- RedisCaffeineCacheManager 本地缓存初始化：");
        if (expireAfterAccess > 0) {
            log.info("cache---------- RedisCaffeineCacheManager 设置本地缓存访问后过期时间，{}秒", expireAfterAccess);
            cacheBuilder.expireAfterAccess(expireAfterAccess, TimeUnit.SECONDS);
        }
        if (expireAfterWrite > 0) {
            log.info("cache---------- RedisCaffeineCacheManager 设置本地缓存写入后过期时间，{}秒", expireAfterWrite);
            cacheBuilder.expireAfterWrite(expireAfterWrite, TimeUnit.SECONDS);
        }
        if (initialCapacity > 0) {
            log.info("cache---------- RedisCaffeineCacheManager 设置缓存初始化大小{}", initialCapacity);
            cacheBuilder.initialCapacity(initialCapacity);
        }
        if (maximumSize > 0) {
            log.info("cache---------- RedisCaffeineCacheManager 设置本地缓存最大值{}", maximumSize);
            cacheBuilder.maximumSize(maximumSize);
        }
        if (refreshAfterWrite > 0) {
            log.info("cache---------- RedisCaffeineCacheManager 设置本地缓存写入后过期时间，{}秒", refreshAfterWrite);
            cacheBuilder.refreshAfterWrite(refreshAfterWrite, TimeUnit.SECONDS);
        }
        cacheBuilder.recordStats();
        return cacheBuilder.build();
    }
}
