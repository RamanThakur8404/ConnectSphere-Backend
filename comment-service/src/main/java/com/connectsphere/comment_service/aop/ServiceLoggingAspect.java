package com.connectsphere.comment_service.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// AOP Logging Aspect for Comment Service.
@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    // -----------------------------------------------------------------------
    // Pointcut — all methods inside any *ServiceImpl class in the service package
    // -----------------------------------------------------------------------

    // Targets every method in CommentServiceImpl (and any future *ServiceImpl).
    @Pointcut("execution(* com.connectsphere.comment_service.service.*ServiceImpl.*(..))")
    public void serviceLayerPointcut() {
        // Pointcut descriptor — no body needed
    }

    // -----------------------------------------------------------------------
    // @Around — logs entry, exit, and execution time
    // -----------------------------------------------------------------------

    // Logs method entry (with arguments), exit (with result type), and
    @Around("serviceLayerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className  = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args     = joinPoint.getArgs();

        log.debug("[AOP] ENTER {}.{}() — args: {}", className, methodName, args);
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long elapsed = System.currentTimeMillis() - start;
        log.debug("[AOP] EXIT  {}.{}() — completed in {} ms", className, methodName, elapsed);

        return result;
    }

    // -----------------------------------------------------------------------
    // @AfterThrowing — logs full stack trace on exception
    // -----------------------------------------------------------------------

    // Logs the full exception stack trace whenever a service method throws.
    @AfterThrowing(pointcut = "serviceLayerPointcut()", throwing = "ex")
    public void logAfterThrowing(org.aspectj.lang.JoinPoint joinPoint, Throwable ex) {
        String className  = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        log.error("[AOP] EXCEPTION in {}.{}() — {}: {}",
                className, methodName, ex.getClass().getSimpleName(), ex.getMessage(), ex);
    }
}
