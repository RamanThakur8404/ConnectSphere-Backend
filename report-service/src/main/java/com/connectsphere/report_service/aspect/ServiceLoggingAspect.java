package com.connectsphere.report_service.aspect;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// Logs method execution for all ServiceImpl classes.
@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

	// Matches all methods inside ServiceImpl classes.
	@Pointcut("execution(* com.connectsphere.report_service.service.impl.*ServiceImpl.*(..))")
	public void serviceImplMethods() {
	}

	// Logs method entry and exit with execution time.
	@Around("serviceImplMethods()")
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
		String methodName = joinPoint.getSignature().toShortString();
		Object[] args = joinPoint.getArgs();

		log.debug("Starting {} with args={}", methodName, Arrays.toString(args));
		long start = System.currentTimeMillis();

		Object result = joinPoint.proceed();

		long elapsed = System.currentTimeMillis() - start;
		log.info("Completed {} in {} ms", methodName, elapsed);

		return result;
	}

	// Logs exception details when a method fails.
	@AfterThrowing(pointcut = "serviceImplMethods()", throwing = "ex")
	public void logException(JoinPoint joinPoint, Exception ex) {
		log.error("Error in {} - {}: {}", joinPoint.getSignature().toShortString(), ex.getClass().getSimpleName(),
				ex.getMessage(), ex);
	}
}
