package org.pw.redisCaffeineCache.support;

import com.alibaba.fastjson.parser.ParserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @summary 缓存监听器
 */
public class CacheMessageListener implements MessageListener {

    private final Logger logger = LoggerFactory.getLogger(CacheMessageListener.class);

    private RedisTemplate<Object, Object> redisTemplate;

    private RedisCaffeineCacheManager redisCaffeineCacheManager;

    {
        //打开json autotype功能
        ParserConfig.getGlobalInstance().addAccept("org.pw.redisCaffeineCache.support.");
    }

    public CacheMessageListener(RedisTemplate<Object, Object> redisTemplate,
                                RedisCaffeineCacheManager redisCaffeineCacheManager) {
        this.redisTemplate = redisTemplate;
        this.redisCaffeineCacheManager = redisCaffeineCacheManager;
    }

    /**
     * 利用 redis 发布订阅通知其他节点清除本地缓存
     *
     * @param message
     * @param pattern
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        CacheMessage cacheMessage = (CacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
        if (cacheMessage.getTimestamp() == null) {
            cacheMessage.setTimestamp(0L);
        }
        if (cacheMessage.getTraceId() != null) {
            MDC.put(TraceIdUtils.TRACE_ID, cacheMessage.getTraceId());
        }
        if (cacheMessage.getMsgId() != null) {
            Object msg = redisCaffeineCacheManager.getLocal(cacheMessage.getCacheName(), cacheMessage.getMsgId());
            if (msg != null) {
                redisCaffeineCacheManager.clearLocal(cacheMessage.getCacheName(), cacheMessage.getMsgId());
                logger.info("cache---------- CacheMessageListen onMessage after {} 毫秒, msgId exists, dropped cacheName:{} key:{}", System.currentTimeMillis() - cacheMessage.getTimestamp(), cacheMessage.getCacheName(), cacheMessage.getKey());
                return;
            }
        }
        logger.info("cache---------- CacheMessageListen onMessage after {} 毫秒, 开始清除本地缓存, cacheName:{} key:{}", System.currentTimeMillis() - cacheMessage.getTimestamp(), cacheMessage.getCacheName(), cacheMessage.getKey());
        redisCaffeineCacheManager.clearLocal(cacheMessage.getCacheName(), cacheMessage.getKey());
        MDC.remove(TraceIdUtils.TRACE_ID);
    }
}
