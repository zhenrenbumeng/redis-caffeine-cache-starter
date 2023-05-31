package org.pw.caffeineCache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class CaffeineCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaffeineCacheApplication.class, args);
    }

}
