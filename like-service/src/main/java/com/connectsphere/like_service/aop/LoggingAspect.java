package com.connectsphere.like_service.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// AOP Logging Aspect for Like Service.
@Aspect
@Component
public class LoggingAspect {

	private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

	// Pointcut: all methods in service.impl package
	@Pointcut("execution(* com.connectsphere.like_service.service.impl.*.*(..))")
	public void serviceImplMethods() {
	}

	// Around advice: logs entry, exit, and execution time
	@Around("serviceImplMethods()")
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

		String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
		String methodName = joinPoint.getSignature().getName();

		log.debug("Starting {}.{}()", className, methodName);

		long start = System.currentTimeMillis();

		try {
			Object result = joinPoint.proceed();

			long time = System.currentTimeMillis() - start;
			log.debug("Completed {}.{}() in {} ms", className, methodName, time);

			return result;

		} catch (Throwable ex) {

			long time = System.currentTimeMillis() - start;
			log.error("Failed {}.{}() after {} ms - {}: {}", className, methodName, time, ex.getClass().getSimpleName(),
					ex.getMessage(), ex);

			throw ex;
		}
	}

	// Logs exception details
	@AfterThrowing(pointcut = "serviceImplMethods()", throwing = "ex")
	public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {

		String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
		String methodName = joinPoint.getSignature().getName();

		log.error("[EXCEPTION] {}.{}() | {}", className, methodName, ex.getMessage());
	}
}