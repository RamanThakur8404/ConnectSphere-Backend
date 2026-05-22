package com.connectsphere.payment_service.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// AOP logging for service layer methods.
@Aspect
@Component
public class AopLoggingAspect {

    // Targets all service implementation methods.
    @Pointcut("execution(* com.connectsphere.payment_service.service.impl.*.*(..))")
    public void serviceLayer() {}

    // Logs method execution and handles exceptions.
    @Around("serviceLayer()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("Starting method: {} | args: {}", methodName, Arrays.toString(args));

        long startMs = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long elapsedMs = System.currentTimeMillis() - startMs;
            log.debug("Completed method: {}() | executionTimeMs={}", methodName, elapsedMs);

            return result;

        } catch (Throwable ex) {

            long elapsedMs = System.currentTimeMillis() - startMs;
            log.error("Failed method: {}() | executionTimeMs={} | message={}",
                    methodName,
                    elapsedMs,
                    ex.getMessage());

            throw ex;
        }
    }

    // Logs full stack trace for exceptions.
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {

        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        log.error("Exception in {}() | type={} | message={}",
                joinPoint.getSignature().getName(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex);
    }
}
