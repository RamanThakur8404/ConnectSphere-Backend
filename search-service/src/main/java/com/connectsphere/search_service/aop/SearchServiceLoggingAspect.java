package com.connectsphere.search_service.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class SearchServiceLoggingAspect {

	private static final Logger logger = LoggerFactory.getLogger(SearchServiceLoggingAspect.class);

	// 
	// Pointcut
	// 

	// All methods in SearchServiceImpl 
	@Pointcut("execution(* com.connectsphere.search_service.serviceimpl.SearchServiceImpl.*(..))")
	public void searchServiceMethods() {
	}

	// 
	// Around — entry, exit, timing
	// 

	// Logs method start, end, and execution time 
	@Around("searchServiceMethods()")
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

		String method = joinPoint.getSignature().toShortString();

		// Preserve existing MDC (important for nested calls)
		String previousRequestId = MDC.get("requestId");

		String requestId = UUID.randomUUID().toString().substring(0, 8);
		MDC.put("requestId", requestId);

		logger.debug("SERVICE_START method={} requestId={}", method, requestId);

		long start = System.currentTimeMillis();

		try {
			Object result = joinPoint.proceed();

			long duration = System.currentTimeMillis() - start;

			logger.debug("SERVICE_END method={} durationMs={} requestId={}", method, duration, requestId);

			return result;

		} finally {
			// Restore previous MDC value
			if (previousRequestId != null) {
				MDC.put("requestId", previousRequestId);
			} else {
				MDC.remove("requestId");
			}
		}
	}

	// 
	// Exception logging
	// 

	// Logs exceptions thrown in service layer 
	@AfterThrowing(pointcut = "searchServiceMethods()", throwing = "ex")
	public void logException(JoinPoint joinPoint, Throwable ex) {

		String method = joinPoint.getSignature().toShortString();

		logger.error("SERVICE_ERROR method={} message={}", method, ex.getMessage(), ex);
	}
}