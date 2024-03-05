# Redis-CaffeineCache分布式二级缓存

> 基于 https://www.cnblogs.com/keeya/p/16556172.html  
> 修正了redisCaffeineCache.clear()未能清除redis、redis订阅本机缓存被清空、NullValue等问题  
> (核心变更见1.0.1-1.0.5版本更新记录)

## module介绍

- redis-caffeine-cache：分布式二级缓存主模块
- caffeine-cache-test：本地缓存测试模块,仅用于测试Caffeine cache本地缓存
- redis-caffeine-cache-test：分布式二级缓存测试模块,实际项目使用可完全参考本模块

## 使用

> 所有配置都可参考redis-caffeine-cache-test模块

### 1.添加dependency 版本以实际最新版本为准

```
<dependency>
    <groupId>org.pw</groupId>
    <artifactId>redis-caffeine-cache</artifactId>
    <version>1.0.8</version>
</dependency>

<repositories>
    <repository>
        <id>lcdt</id>
        <name>lcdt</name>
        <url>http://101.200.215.10:8083/repository/cloud-3party/</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
<repositories>
```

### 2.原项目redis配置

> 二级缓存使用原有的Redis配置，参考redis-caffeine-cache-test模块config部分。  
> 核心是配置一个名为`redisTemplate4L2Cache`的bean，用于二级缓存使用。  
> ParserConfig.getGlobalInstance().addAccept("org.springframework.cache.support.NullValue");//支持缓存NullValue

<details>

<summary><mark>参考内容</mark></summary>

#### 2.1 情形1：

> 定义bean`redisTemplate4L2Cache`

``` 
    @Bean
    public RedisTemplate<Object, Object> redisTemplate4L2Cache(RedisConnectionFactory connectionFactory) {
        return redisTemplate(connectionFactory);
    }

    @Bean
    @Primary
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);
        ParserConfig.getGlobalInstance().addAccept("org.springframework.cache.support.NullValue");
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
```

#### 2.2 情形2

> 原项目中已有了`redisTemplate`bean ：  
> 1 原有`redisTemplate`添加一个 @primary 注解  
> 2 使用下面的`redisTemplate4L2Cache` bean.

``` 
    @Bean
    public RedisTemplate<Object, Object> redisTemplate4L2Cache(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);
        ParserConfig.getGlobalInstance().addAccept("org.springframework.cache.support.NullValue");
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

    @Primary
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
        RedisSerializer redisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(redisSerializer);
        redisTemplate.setValueSerializer(redisSerializer);
        redisTemplate.setHashKeySerializer(redisSerializer);
        redisTemplate.setHashValueSerializer(redisSerializer);
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }
```

#### 2.3 其它情形

必须满足以下条件
> When enabling cacheNullValues please make sure the RedisSerializer used by RedisOperations is capable of serializing NullValue.

</details>

### 3.启动类配置

```
@EnableCaching
@SpringBootApplication(scanBasePackages = {"org.pw.redisCaffeineCache"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### ~~4.配置文件~~

> 此项废弃，1.0.8版本可以不需要配置

<details> 

<summary><mark>默认配置</mark></summary>

配置如下
> 1 配置不同的cache.redisCaffeineCache.cachePrefix 用于redis中不同的命名空间  
> 2 配置不同的cache.redisCaffeineCache.redis.topic 用于区分不同项目清除本地缓存消息

``` 
cache.redisCaffeineCache:
  cachePrefix: redis-caffeine-cache #缓存key前缀
  dynamic: true #是否动态根据cacheName创建Cache的实现，默认true
  redis:
    topic: redisCaffeine:topic
    defaultExpiration: 3600 #二级缓存默认redis过期时间，单位秒，默认3600s
  # 针对自定义cacheName的本地一级缓存配置
  cacheDefault:
    expireAfterAccess: 86400 #访问后过期时间，单位秒
    expireAfterWrite: 86400 #写入后过期时间，单位秒 1天
    initialCapacity: 10000 #初始化大小
    maximumSize: 100000 #最大缓存对象个数，超过此数量时会使用Window TinyLfu策略来淘汰缓存

  # 默认写入后过期时间，单位秒 expireAfterWrite = 120;
  #	默认初始化大小 initialCapacity = 50;
  #	默认最大缓存对象个数 maximumSize = 50;
  cache1m:
    expireAfterAccess: 60
    expireAfterWrite: 60
    initialCapacity: 10000
    maximumSize: 100000
  cache5m:
    expireAfterAccess: 300
    expireAfterWrite: 300
    initialCapacity: 10000
    maximumSize: 100000
  cache15m:
    expireAfterAccess: 900
    expireAfterWrite: 900
    initialCapacity: 10000
    maximumSize: 100000
  cache60m:
    expireAfterAccess: 3600 #访问后过期时间，单位秒
    expireAfterWrite: 3600 #写入后过期时间，单位秒 1小时
    initialCapacity: 10000 #初始化大小
    maximumSize: 100000 #最大缓存对象个数，超过此数量时会使用Window TinyLfu策略来淘汰缓存
  cache12h:
    expireAfterAccess: 43200 #访问后过期时间，单位秒
    expireAfterWrite: 43200 #写入后过期时间，单位秒 12小时
    initialCapacity: 10000 #初始化大小
    maximumSize: 100000 #最大缓存对象个数，超过此数量时会使用Window TinyLfu策略来淘汰缓存
  cache24h:
    expireAfterAccess: 86400 #访问后过期时间，单位秒
    expireAfterWrite: 86400 #写入后过期时间，单位秒 1天
    initialCapacity: 10000 #初始化大小
    maximumSize: 100000 #最大缓存对象个数，超过此数量时会使用Window TinyLfu策略来淘汰缓存
  cachePermanent:
    expireAfterAccess: 8640000 #访问后过期时间，单位秒
    expireAfterWrite: 8640000 #写入后过期时间，单位秒 100天
    initialCapacity: 10000 #初始化大小
    maximumSize: 100000 #最大缓存对象个数，超过此数量时会使用Window TinyLfu策略来淘汰缓存
```

</details>

### 5.基本使用

> 更多方法参考redis-caffeine-cache-test模块Controller类

<details> <summary><mark>基本使用参考</mark></summary>

##### 5.1 @Cacheable 存入缓存

- cacheManager 保持不变
- cacheNames 可选为1分钟、5分钟、15分钟、1小时、12小时、24小时、永久（100天）
- key 保证缓存唯一性，支持SpEL: #user.id
- sync @Cacheable使用，避免缓存击穿

> 如配置为  
> cache.redisCaffeineCache.cachePrefix=redis-caffeine-cache #缓存key前缀  
> @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_24HOUR, key = "'user'+#id", sync=true)  
> id=1,则生成的缓存位置为: redis-caffeine-cache:cache:24h:user1

``` 
    //查询时存入缓存
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_24HOUR, key = "'user'+#id", sync=true)
    public User getUser(Integer id) {
        log.info("new user");
        user.setId(id);
        user.setName("初始值" + sdf.format(new Date()));
        return user;
    }
    
```

##### 5.1 @CachePut 更新缓存

``` 
    //更新方法，更新缓存
    @CachePut(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_24HOUR, key = "'user'+#id")
    public User updateUser(Integer id, String name) {
        user.setId(id);
        user.setName(name + sdf.format(new Date()));
        return user;
    }
    
```

##### 5.2 @CacheEvict 删除缓存

``` 
    //删除时废弃缓存
    @CacheEvict(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_24HOUR, key = "'user'+#id")
    public User delete(Integer id) {
        user.setId(id);
        user.setName(null);
        return user;
    }
    
```

</details>

### 可选：~~屏蔽redis-caffeine-cache日志~~

1.0.7版日志已优化，只记录put日志，不需要屏蔽。

~~logback-spring.xml中配置~~

```
    <logger name="org.pw.redisCaffeineCache" level="OFF"/>
```

## 总结内容

. 防止缓存穿透(key在缓存中是不存在的)：已默认配置了 `allowNullValues=true;`:存储null值。  
. 防止缓存击穿(高并发访问，key不存在):@Cacheable添加sync=true

## 版本更新日志

> 1.0.8    
> 增加默认配置，不再需要编辑配置文件  
> 修改了一些配置项的名称，参考配置文件中已注释掉的默认配置：主要将缓存名称改为l2cache开头.  
> 有效配置是写入后过期时间，访问后过期时间设置测试无效

<details> <summary><mark>更多更新记录</mark></summary>

> > 1.0.7  
> 精简log：  
> 前缀统一为L2_CacheManager  
> 除初始化等log，只要使用缓存总是会触发`put`，所以核心只保留put log： `L2_CacheManager RedisCaffeineCache put key:[xx],value:{xx}`  
> lookup、get等日志降级为DEBUG  
> evict、clear等日志降级为DEBUG  
> 因缓存过期没有日志，CacheMessageListener.onMessage() 主动清理缓存（@Cacheable、@CacheEvict、@CachePut）日志降级为DEBUG


> 1.0.6  
> 1分钟缓存：Cachenames.CACHE_1MIN

> 1.0.5  
> 兼容老项目：缓存使用的`redisTemplate` 替换为`redisTemplate4L2Cache`,以保留原项目默认的`redisTemplate`  
> 参考 2.原项目Redis配置

> 1.0.4  
> 缓存NullValue问题修正-补充 从Redis取得NullValue返回null.

> 1.0.3  
> 缓存NullValue问题修正:  
> -NullValue解析修正：配置fastjson Accept， 见RedisConfig  
> -RedisCaffeineCache.get() NullValue返回null.

> 1.0.2  
> 日志格式统一、优化; trace_id完善;

> 1.0.1  
> 修正缓存put时未放入本地缓存问题：增加{msgId:0}到本地缓存，收到消息后判断如果本地缓存存在则invalid msgId，不清空本地缓存

> 1.0.0  
> 初版
>
</details>

## 内存占用情况直观感受

> 1条物流公司记录 456B  
> 1978条物流公司记录 768KB

```json
{
  "@type": "com.lensung.distribution.trade.po.ExpressCompanyPO",
  "brandCode": "",
  "companyCode": "ZTO",
  "companyName": "中通快递",
  "companyType": 1,
  "createTime": "2022-05-17T17:48:03",
  "id": 94,
  "logisticsCode": "9",
  "logisticsType": 1,
  "often": false,
  "pinyin": "",
  "platform": "ksxd",
  "supportParent": false,
  "updateTime": "2022-10-08T14:14:18",
  "waybillNoReg": "",
  "waybillPlatform": "ksxd"
}

```
