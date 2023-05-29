# Redis-CaffeineCache分布式二级缓存

> 基于 https://www.cnblogs.com/keeya/p/16556172.html 修正了redisCaffeineCache.clear()未能清除redis等问题

## module介绍

- redis-caffeine-cache 分布式二级缓存主模块
- redis-caffeine-cache-test 仅用于测试

## 使用

> 所有配置都可参考redis-caffeine-cache-test

### 1.添加dependency 版本以实际最新版本为准

```
<dependency>
    <groupId>org.pw</groupId>
    <artifactId>redis-caffeine-cache</artifactId>
    <version>1.0.0</version>
</dependency>

<repositories>
    <repository>
        <id>lcdt</id>
        <name>lcdt</name>
        <url>http://101.200.215.10:8083/repository/3party-snapshot/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
<repositories>
```

### 2.原项目redis配置

二级缓存使用原有的Redis配置

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

### 4.配置文件

配置如下

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

### 5.基本使用

> 更多方法参考redis-caffeine-cache-test模块Controller类

##### 5.1 @Cacheable 存入缓存

- cacheManager 保持不变
- cacheNames 可选为5分钟、15分钟、1小时、12小时、24小时、永久（100天）
- key 保证缓存唯一性，支持SpEL: #dto.id

``` 
    //查询时存入缓存
    @Cacheable(cacheManager = "L2_CacheManager", cacheNames = CacheNames.CACHE_24HOUR, key = "'user'+#id")
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

### 可选：屏蔽redis-caffeine-cache日志

logback-spring.xml中配置

```
    <logger name="org.pw.redisCaffeineCache" level="OFF"/>
```

## 版本更新日志

> 1.0.0  
> 初版



