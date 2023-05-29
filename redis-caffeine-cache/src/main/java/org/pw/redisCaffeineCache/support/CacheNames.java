package org.pw.redisCaffeineCache.support;

public interface CacheNames {
    /**
     * 5分钟缓存组
     */
    String CACHE_5MINS = "cache:5m";
    /**
     * 15分钟缓存组
     */
    String CACHE_15MINS = "cache:15m";
    /**
     * 60分钟缓存组
     */
    String CACHE_60MINS = "cache:60m";
    /**
     * 12Hours分钟缓存组
     */
    String CACHE_12HOUR = "cache:12h";
    /**
     * 24Hours分钟缓存组
     */
    String CACHE_24HOUR = "cache:24h";
    /**
     * 长期、永久
     */
    String CACHE_PERMANENT = "cache:permanent";
}