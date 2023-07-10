package org.pw.redisCaffeineCache.support;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.pw.redisCaffeineCache.CacheRedisCaffeineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RedisCaffeineCache extends AbstractValueAdaptingCache {

    private final Logger logger = LoggerFactory.getLogger(RedisCaffeineCache.class);

    private String name;

    private RedisTemplate<Object, Object> redisTemplate;

    @Getter
    private Cache<Object, Object> caffeineCache;

    private String cachePrefix;

    /**
     * 默认key超时时间 1h:3600s
     */
    private long defaultExpiration = 3600;

    private Map<String, Long> defaultExpires = new HashMap<>();

    {
        defaultExpires.put(CacheNames.CACHE_1MIN, TimeUnit.MINUTES.toSeconds(1));
        defaultExpires.put(CacheNames.CACHE_5MINS, TimeUnit.MINUTES.toSeconds(5));
        defaultExpires.put(CacheNames.CACHE_15MINS, TimeUnit.MINUTES.toSeconds(15));
        defaultExpires.put(CacheNames.CACHE_60MINS, TimeUnit.MINUTES.toSeconds(60));
        defaultExpires.put(CacheNames.CACHE_12HOUR, TimeUnit.HOURS.toSeconds(12));
        defaultExpires.put(CacheNames.CACHE_24HOUR, TimeUnit.HOURS.toSeconds(24));
    }

    private String topic;
    private Boolean logShowValue = true;
    private Map<String, ReentrantLock> keyLockMap = new ConcurrentHashMap();

    protected RedisCaffeineCache(boolean allowNullValues) {
        super(allowNullValues);
    }

    public RedisCaffeineCache(String name, RedisTemplate<Object, Object> redisTemplate,
                              Cache<Object, Object> caffeineCache, CacheRedisCaffeineProperties cacheRedisCaffeineProperties) {
        super(cacheRedisCaffeineProperties.isCacheNullValues());
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.caffeineCache = caffeineCache;
        this.cachePrefix = cacheRedisCaffeineProperties.getCachePrefix();
        this.defaultExpiration = cacheRedisCaffeineProperties.getRedis().getDefaultExpiration();
        this.topic = cacheRedisCaffeineProperties.getRedis().getTopic();
        this.logShowValue = cacheRedisCaffeineProperties.isLogShowValue();
        defaultExpires.putAll(cacheRedisCaffeineProperties.getRedis().getExpires());
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = lookup(key);
        if (value != null) {
            if (value instanceof NullValue) {
                return null;
            }
            return (T) value;
        }
        //key在redis和缓存中均不存在
        ReentrantLock lock = keyLockMap.get(key.toString());

        if (lock == null) {
            logger.debug("L2_CacheManager RedisCaffeineCache create lock for key : {}", key);
            keyLockMap.putIfAbsent(key.toString(), new ReentrantLock());
            lock = keyLockMap.get(key.toString());
        }
        try {
            lock.lock();
            value = lookup(key);
            if (value != null) {
                if (value instanceof NullValue) {
                    return null;
                }
                return (T) value;
            }
            //执行原方法获得value
            value = valueLoader.call();
            Object storeValue = toStoreValue(value);
            put(key, storeValue);
            return (T) value;
        } catch (Exception e) {
            //logger.error("L2_CacheManager RedisCaffeineCache get error", e);
            throw new ValueRetrievalException(key, valueLoader, e.getCause());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (!super.isAllowNullValues() && value == null) {
            this.evict(key);
            return;
        }
        long expire = getExpire();
        redisTemplate.opsForValue().set(getKey(key), toStoreValue(value), expire, TimeUnit.SECONDS);
        logger.debug("L2_CacheManager RedisCaffeineCache put 二级缓存 key:[{}] expire:{} value:{}", getKey(key), Duration.ofSeconds(expire), JSONObject.toJSONString(value));

        //缓存变更时通知其他节点清理本地缓存
        String msgId = TraceIdUtils.MSGID_CACHE_KEY_PREFIX + UUID.randomUUID();
        push(new CacheMessage(msgId, this.name, key, MDC.get(TraceIdUtils.TRACE_ID)));
        String traceId = MDC.get(TraceIdUtils.TRACE_ID);
        caffeineCache.put(msgId, traceId != null ? traceId : msgId);
        logger.debug("L2_CacheManager RedisCaffeineCache put 本地缓存 key:[{}],value:{}", key, JSONObject.toJSONString(value));
        caffeineCache.put(key, toStoreValue(value));
        logger.info("L2_CacheManager RedisCaffeineCache put key:[{}],value:{}", key, JSONObject.toJSONString(value));
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Object cacheKey = getKey(key);
        // 使用setIfAbsent原子性操作
        long expire = getExpire();
        boolean setSuccess;
        setSuccess = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(getKey(key), toStoreValue(value), Duration.ofSeconds(expire)));
        Object hasValue;
        //setNx结果
        if (setSuccess) {
            push(new CacheMessage(this.name, key, MDC.get(TraceIdUtils.TRACE_ID)));
            hasValue = value;
        } else {
            hasValue = redisTemplate.opsForValue().get(cacheKey);
        }
        caffeineCache.put(key, toStoreValue(value));
        logger.info("L2_CacheManager RedisCaffeineCache put 本地缓存 key:[{}],value:{}", key, JSONObject.toJSONString(value));
        return toValueWrapper(hasValue);
    }

    @Override
    public void evict(Object key) {
        logger.debug("L2_CacheManager RedisCaffeineCache evict key:[{}]", key);

        // 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        redisTemplate.delete(getKey(key));

        push(new CacheMessage(this.name, key, MDC.get(TraceIdUtils.TRACE_ID)));

        caffeineCache.invalidate(key);
    }

    @Override
    public void clear() {
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        String cachePrefix = this.cachePrefix + ":";
        if (StringUtils.isEmpty(this.cachePrefix)) {
            cachePrefix = "";
        }
        String s = cachePrefix + this.name.concat(":*");
        Set<Object> keys = redisTemplate.keys(s);
        for (Object key : keys) {
            redisTemplate.delete(key.toString());
        }
        push(new CacheMessage(this.name, null, MDC.get(TraceIdUtils.TRACE_ID)));
        caffeineCache.invalidateAll();
    }

    /**
     * 取值逻辑
     *
     * @param key
     * @return
     */
    @Override
    protected Object lookup(Object key) {
        Object cacheKey = getKey(key);
        Object value = caffeineCache.getIfPresent(key);
        if (value != null) {
            if (this.logShowValue) {
                if (value instanceof NullValue) {
                    logger.debug("L2_CacheManager RedisCaffeineCache 从本地缓存中获得key:[{}],value:NullValue", key);
                } else {
                    logger.debug("L2_CacheManager RedisCaffeineCache 从本地缓存中获得key:[{}],value:{}", key, JSONObject.toJSONString(value));
                }
            } else {
                logger.debug("L2_CacheManager RedisCaffeineCache 从本地缓存中获得key:[{}]", key);
            }
            return value;
        }
        value = redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            if (this.logShowValue) {
                if (value instanceof NullValue) {
                    logger.debug("L2_CacheManager RedisCaffeineCache 从二级缓存中获得key:[{}],value:NullValue", key);
                } else {
                    logger.debug("L2_CacheManager RedisCaffeineCache 从二级缓存中获得key:[{}],value:{}", cacheKey, JSONObject.toJSONString(value));
                }
            } else {
                logger.debug("L2_CacheManager RedisCaffeineCache 从二级缓存中获得key:[{}]", cacheKey);
            }
            caffeineCache.put(key, value);
        }
        return value;
    }

    /**
     * @description 清理本地缓存
     */
    public void clearLocal(Object key) {
        if (key == null) {
            caffeineCache.invalidateAll();
        } else {
            if (!key.toString().startsWith(TraceIdUtils.MSGID_CACHE_KEY_PREFIX)) {
                logger.debug("L2_CacheManager RedisCaffeineCache 清理本地缓存 key:[{}]", key);
            }
            caffeineCache.invalidate(key);
        }
    }

    //————————————————————————————私有方法——————————————————————————

    private Object getKey(Object key) {
        String keyStr = this.name.concat(":").concat(key.toString());
        return StringUtils.isEmpty(this.cachePrefix) ? keyStr : this.cachePrefix.concat(":").concat(keyStr);
    }

    private long getExpire() {
        long expire = defaultExpiration;
        Long cacheNameExpire = defaultExpires.get(this.name);
        return cacheNameExpire == null ? expire : cacheNameExpire;
    }

    /**
     * @description 缓存变更时通知其他节点清理本地缓存
     */
    private void push(CacheMessage message) {
        logger.debug("L2_CacheManager RedisCaffeineCache push cacheMessage,{}", JSONObject.toJSONString(message));
        redisTemplate.convertAndSend(topic, message);
    }

    /**
     * @description 调试方法，显示缓存
     */
    private void showCaches() {
        JSONArray jsonArray = new JSONArray();
        ConcurrentMap<@NonNull Object, @NonNull Object> objectObjectConcurrentMap = caffeineCache.asMap();
        JSONObject obj = new JSONObject();
        obj.put(this.name, objectObjectConcurrentMap);
        jsonArray.add(obj);
        logger.info("L2_CacheManager RedisCaffeineCache showCaches:{}", JSONObject.toJSONString(jsonArray, true));
    }

}
