package com.connectsphere.media_service.aop;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

	private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

	// Pointcut: all service implementation methods. 
	@Pointcut("execution(* com.connectsphere.media_service.service.impl.*ServiceImpl.*(..))")
	public void serviceImplMethods() {
	}

	// Pointcut: all repository methods. 
	@Pointcut("execution(* com.connectsphere.media_service.repository.*.*(..))")
	public void repositoryMethods() {
	}

	// Pointcut: all controller methods. 
	@Pointcut("execution(* com.connectsphere.media_service.controller.*.*(..))")
	public void controllerMethods() {
	}

	// Around — service layer

	// Logs service method entry, exit, and execution time.
	@Around("serviceImplMethods()")
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
		String method = joinPoint.getSignature().toShortString();
		log.debug("Entering {} | args={}", method, Arrays.toString(joinPoint.getArgs()));

		long start = System.currentTimeMillis();
		Object result = joinPoint.proceed();
		long elapsed = System.currentTimeMillis() - start;

		log.debug("Exiting {} | time={} ms", method, elapsed);
		return result;
	}

	// Logs exceptions thrown from service methods with full context.
	@AfterThrowing(pointcut = "serviceImplMethods()", throwing = "ex")
	public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {
		log.error("Exception in {} | args={} | error={}", joinPoint.getSignature().toShortString(),
				Arrays.toString(joinPoint.getArgs()), ex.getMessage(), ex);
	}

	// Before — repository layer

	@Before("repositoryMethods()")
	public void logRepositoryEntry(JoinPoint joinPoint) {
		log.debug("[REPOSITORY] {}.{}() args={}", joinPoint.getTarget().getClass().getSimpleName(),
				joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
	}

	// Before / AfterThrowing — controller layer

	@Before("controllerMethods()")
	public void logControllerRequest(JoinPoint joinPoint) {
		log.info("[CONTROLLER] {}.{}() args={}", joinPoint.getTarget().getClass().getSimpleName(),
				joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
	}

	@AfterThrowing(pointcut = "controllerMethods()", throwing = "ex")
	public void logControllerException(JoinPoint joinPoint, Exception ex) {
		log.error("[CONTROLLER] Exception in {}.{}(): {}", joinPoint.getTarget().getClass().getSimpleName(),
				joinPoint.getSignature().getName(), ex.getMessage());
	}
}
