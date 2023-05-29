package org.pw.redisCaffeineCache;

import lombok.extern.slf4j.Slf4j;
import org.pw.redisCaffeineCache.support.CacheMessageListener;
import org.pw.redisCaffeineCache.support.RedisCaffeineCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import javax.annotation.Resource;
import java.util.Objects;


@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(CacheRedisCaffeineProperties.class)
@Slf4j
public class CacheRedisCaffeineAutoConfiguration {
    @Resource
    RedisTemplate redisTemplate;

    @Autowired
    private CacheRedisCaffeineProperties cacheRedisCaffeineProperties;

    @Bean("L2_CacheManager")
    @ConditionalOnBean(RedisTemplate.class)
    public RedisCaffeineCacheManager cacheManager() {
        log.info("===========================================");
        log.info("=                                         =");
        log.info("=          Two Level Cache Start          =");
        log.info("=                                         =");
        log.info("===========================================");
        return new RedisCaffeineCacheManager(cacheRedisCaffeineProperties, redisTemplate);
    }

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<Object, Object> stringRedisTemplate, RedisCaffeineCacheManager redisCaffeineCacheManager) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(Objects.requireNonNull(stringRedisTemplate.getConnectionFactory()));
        CacheMessageListener cacheMessageListener = new CacheMessageListener(stringRedisTemplate, redisCaffeineCacheManager);
        redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(cacheRedisCaffeineProperties.getRedis().getTopic()));
        return redisMessageListenerContainer;
    }
}
