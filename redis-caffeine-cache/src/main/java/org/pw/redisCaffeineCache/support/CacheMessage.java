package org.pw.redisCaffeineCache.support;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class CacheMessage implements Serializable {

    private static final long serialVersionUID = -1L;

    private String cacheName;

    private Object key;

    private String msgId;
    /**
     * 日志跟踪
     */
    private String traceId;

    public CacheMessage(String cacheName, Object key, String traceId) {
        super();
        this.cacheName = cacheName;
        this.key = key;
        this.traceId = traceId;
    }

    public CacheMessage(String msgId, String cacheName, Object key, String traceId) {
        super();
        this.msgId = msgId;
        this.cacheName = cacheName;
        this.key = key;
        this.traceId = traceId;
    }
}
