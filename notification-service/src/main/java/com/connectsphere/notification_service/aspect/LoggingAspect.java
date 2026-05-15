package com.connectsphere.notification_service.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// AOP Logging Aspect for service layer.
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // Pointcut: all service implementation methods
    @Pointcut("execution(* com.connectsphere.notification_service.service.impl.*ServiceImpl.*(..))")
    public void serviceImplMethods() {}

    // Logs method entry, exit, and execution time
    @Around("serviceImplMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

        String method = joinPoint.getSignature().toShortString();

        log.debug("Entering {} | args={}", method, Arrays.toString(joinPoint.getArgs()));

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long time = System.currentTimeMillis() - start;

        log.debug("Exiting {} | time={} ms", method, time);

        return result;
    }

    // Logs exceptions with stack trace
    @AfterThrowing(pointcut = "serviceImplMethods()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {

        log.error(" Exception in {} | args={} | error={}",
                joinPoint.getSignature().toShortString(),
                Arrays.toString(joinPoint.getArgs()),
                ex.getMessage(),
                ex);
    }
}