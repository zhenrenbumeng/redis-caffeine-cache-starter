package org.pw.l2cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pw.l2cache.controller.Controller;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 定时任务测试
 *
 * @author @yangf.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = L2cacheTestApplication.class)
@ActiveProfiles("")
@Slf4j
public class CacheTests {


    @Resource
    Controller controller;

    @Test
    public void test() {
        controller.test(1);
    }
}
