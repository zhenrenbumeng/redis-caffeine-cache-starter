package org.pw.l2cache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(scanBasePackages = {"org.pw.redisCaffeineCache", "org.pw.l2cache"})
public class L2cacheTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(L2cacheTestApplication.class, args);
    }

}
