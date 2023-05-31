package org.pw.caffeineCache.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@Aspect
@Component
@Slf4j
// @Order(-2)
// @ConditionalOnProperty
public class LogAspect {

    public static final String TRACE_ID = "trace_id";

    @Before("@within(restController)")
    public void log(JoinPoint point, RestController restController) {
        MDC.put(TRACE_ID, UUID.randomUUID().toString());
    }
}
