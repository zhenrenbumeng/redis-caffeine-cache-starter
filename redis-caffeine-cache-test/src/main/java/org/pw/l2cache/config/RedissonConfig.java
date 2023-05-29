package org.pw.l2cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author lf
 * @create: 2022/4/13
 * @description:
 */
@Component
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfig {

    private String host;

    private String password;

    private Integer port;

    private Integer redissonDataBase;

    private Integer redissonThreads;

    private Integer redissonNettyThreads;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getRedissonDataBase() {
        return redissonDataBase;
    }

    public void setRedissonDataBase(Integer redissonDataBase) {
        this.redissonDataBase = redissonDataBase;
    }

    public Integer getRedissonThreads() {
        return redissonThreads;
    }

    public void setRedissonThreads(Integer redissonThreads) {
        this.redissonThreads = redissonThreads;
    }

    public Integer getRedissonNettyThreads() {
        return redissonNettyThreads;
    }

    public void setRedissonNettyThreads(Integer redissonNettyThreads) {
        this.redissonNettyThreads = redissonNettyThreads;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
