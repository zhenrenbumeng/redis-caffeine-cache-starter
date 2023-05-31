package org.pw.caffeineCache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine本地缓存策略
 *
 * @author @yangf.
 */
@Configuration
@EnableCaching
public class CaffeineCacheConfig {
    /**
     * 通用本地缓存
     *
     * @return
     */
    @Bean(name = "commonECManager")
    public CacheManager commonCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
          .expireAfterWrite(1, TimeUnit.DAYS)
          .maximumSize(10000);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    /**
     * 重要数据 本地缓存，如
     *
     * @return
     */
    @Primary
    @Bean(name = "importantCacheManager")
    public CacheManager importantCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
          .expireAfterWrite(1, TimeUnit.DAYS)
          .maximumSize(100000);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

}
