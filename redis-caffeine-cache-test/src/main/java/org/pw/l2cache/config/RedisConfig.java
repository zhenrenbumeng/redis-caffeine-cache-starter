package org.pw.l2cache.config;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
/**
 * redis配置
 *
 * @author lif
 */
@Configuration
@Slf4j
public class RedisConfig {
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);
        ParserConfig.getGlobalInstance().addAccept("com.lensung.trade.");
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        serializer.setObjectMapper(mapper);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedissonClient redissonClient(RedissonConfig redissonConfig) {
        log.info(JSONObject.toJSONString(redissonConfig));
        if ("localhost".equals(redissonConfig.getHost()) || "127.0.0.1".equals(redissonConfig.getHost())) {
            return Redisson.create();
        }
        Config config = new Config();
        config.setCodec(new StringCodec());
        String address = "redis://" + redissonConfig.getHost() + ":" + redissonConfig.getPort();
        config.useSingleServer().setAddress(address).setDatabase(redissonConfig.getRedissonDataBase());
        if (StringUtils.isNotEmpty(redissonConfig.getPassword())) {
            config.useSingleServer().setPassword(redissonConfig.getPassword());
        }
        config.setNettyThreads(redissonConfig.getRedissonNettyThreads());
        config.setThreads(redissonConfig.getRedissonThreads());
        return Redisson.create(config);
    }

    @Bean
    public DefaultRedisScript<Long> limitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(limitScriptText());
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 限流脚本
     */
    private String limitScriptText() {
        return "local key = KEYS[1]\n" + "local count = tonumber(ARGV[1])\n" + "local time = tonumber(ARGV[2])\n" + "local current = redis.call('get', key);\n" + "if current and tonumber(current) > count then\n" + "    return tonumber(current);\n" + "end\n" + "current = redis.call('incr', key)\n" + "if tonumber(current) == 1 then\n" + "    redis.call('expire', key, time)\n" + "end\n" + "return tonumber(current);";
    }
}
