package org.pw.l2cache;

import lombok.extern.slf4j.Slf4j;
import org.pw.l2cache.service.UserServiceImpl;
import org.pw.redisCaffeineCache.support.CacheNames;
import org.pw.redisCaffeineCache.support.RedisCaffeineCacheManager;
import org.pw.redisCaffeineCache.support.TraceIdUtils;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Slf4j
@Component
@EnableScheduling
public class ScheduleTask {

    @Resource
    RedisCaffeineCacheManager redisCaffeineCacheManager;

    @Resource
    UserServiceImpl userService;

    /**
     * 并发测试
     */
    // @Scheduled(fixedDelay = 1000 * 20) //20秒
    public void test() {
        System.out.println();
        for (int i = 0; i < 3; i++) {
            //多线程运行
            int finalI = i;
            new Thread(() -> {
                MDC.put(TraceIdUtils.TRACE_ID, UUID.randomUUID().toString());
                //    1-100随机数
                userService.getUser(finalI);
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                userService.updateUser(finalI, finalI + "_");
                userService.getUser(finalI);
            }).start();
        }
        redisCaffeineCacheManager.allCaches(CacheNames.CACHE_1MIN);
    }

}
