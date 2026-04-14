package com.epam.rd.autocode.spring.project.aop;

import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.epam.rd.autocode.spring.project.service..*(..))")
    public void serviceMethods() {}


    @Around("serviceMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();


        if (log.isDebugEnabled()) {
            log.debug(" [ENTRY] {}.{}() з аргументами: {}", className, methodName, sanitizeArgs(joinPoint.getArgs()));
        } else {
            log.info("️ [ENTRY] {}.{}()", className, methodName);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();

            stopWatch.stop();
            log.info(" [EXIT] {}.{}() виконано успішно за {} мс", className, methodName, stopWatch.getTotalTimeMillis());

            return result;

        } catch (Throwable exception) {
            stopWatch.stop();
            log.error(" [ERROR] {}.{}() впав через {} мс. Причина: {}",
                    className, methodName, stopWatch.getTotalTimeMillis(), exception.getMessage());
            throw exception;
        }
    }


    private String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return java.util.Arrays.toString(args);
    }
}