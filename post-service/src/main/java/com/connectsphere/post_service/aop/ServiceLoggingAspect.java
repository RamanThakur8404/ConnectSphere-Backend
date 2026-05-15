package com.connectsphere.post_service.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    @Pointcut("execution(* com.connectsphere.post_service.service.PostServiceImpl.*(..))")
    public void postServiceMethods() {}

    @Around("postServiceMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();

        log.info("Starting method: {}", method);

        Object result = joinPoint.proceed();

        long time = System.currentTimeMillis() - start;
        log.info("Completed method: {} in {}ms", method, time);

        return result;
    }
}