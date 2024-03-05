package org.pw.redisCaffeineCache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "cache.l2cache")
@Data
public class CacheRedisCaffeineProperties {

    /**
     * 是否存储空值，默认true，防止缓存穿透
     */
    private boolean cacheNullValues = true;

    /**
     * 是否动态根据cacheName创建Cache的实现，默认true
     */
    private boolean dynamic = true;
    /**
     * RedisCaffeineCache 日志中是否输出value
     */
    private boolean logShowValue = true;

    /**
     * 缓存key的前缀
     */
    private String cachePrefix="l2cache";

    private Redis redis = new Redis();

    private CacheDefault cacheDefault = new CacheDefault();
    private Cache1m cache1m = new Cache1m();
    private Cache5m cache5m = new Cache5m();
    private Cache15m cache15m = new Cache15m();
    private Cache60m cache60m = new Cache60m();
    private Cache12h cache12h = new Cache12h();
    private Cache24h cache24h = new Cache24h();
    private CachePermanent cachePermanent = new CachePermanent();

    @Data
    public class Redis {

        /**
         * 全局过期时间，单位秒，默认不过期
         */
        private long defaultExpiration = 0;

        /**
         * 每个cacheName的过期时间，单位秒，优先级比defaultExpiration高
         */
        private Map<String, Long> expires = new HashMap<>();

        /**
         * 缓存更新时通知其他节点的topic名称
         */
        private String topic = "l2cache:topic";

    }

    @Data
    public class CacheDefault {
        /**
         * 访问后过期时间，单位秒
         */
        protected long expireAfterAccess;
        /**
         * 写入后过期时间，单位秒
         */
        protected long expireAfterWrite = 120;
        /**
         * 写入后刷新时间，单位秒
         */
        protected long refreshAfterWrite;
        /**
         * 初始化大小,默认50
         */
        protected int initialCapacity = 50;
        /**
         * 最大缓存对象个数
         */
        protected long maximumSize = 50;

        /** 由于权重需要缓存对象来提供，对于使用spring cache这种场景不是很适合，所以暂不支持配置*/
//		private long maximumWeight;
    }

    public class Cache1m extends CacheDefault {
        public Cache1m() {
            expireAfterAccess = 60;
            expireAfterWrite = 60;
            initialCapacity = 1000;
            maximumSize = 100000;
        }
    }

    public class Cache5m extends CacheDefault {
        public Cache5m() {
            expireAfterAccess = 300;
            expireAfterWrite = 300;
            initialCapacity = 1000;
            maximumSize = 100000;
        }
    }

    public class Cache15m extends CacheDefault {
        public Cache15m() {
            expireAfterAccess = 900;
            expireAfterWrite = 900;
            initialCapacity = 1000;
            maximumSize = 100000;
        }
    }

    public class Cache60m extends CacheDefault {
        public Cache60m() {
            expireAfterAccess = 3600;
            expireAfterWrite = 3600;
            initialCapacity = 1000;
            maximumSize = 100000;
        }
    }

    public class Cache12h extends CacheDefault {
        public Cache12h() {
            expireAfterAccess = 43200;
            expireAfterWrite = 43200;
            initialCapacity = 1000;
            maximumSize = 100000;
        }
    }

    public class Cache24h extends CacheDefault {
        public Cache24h() {
            expireAfterAccess = 86400;
            expireAfterWrite = 86400;
            initialCapacity = 1000;
            maximumSize = 100000;
        }
    }

    public class CachePermanent extends CacheDefault {
        public CachePermanent() {
            expireAfterAccess = -1;
            expireAfterWrite = -1;
            initialCapacity = 1000;
            maximumSize = 100000;
        }
    }
}
