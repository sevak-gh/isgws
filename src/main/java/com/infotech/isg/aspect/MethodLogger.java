package com.infotech.isg.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * generic method entry logger.
 *
 * @author Sevak Gharibian
 */
@Aspect
@Component
public class MethodLogger {

    private static final Logger LOG = LoggerFactory.getLogger(MethodLogger.class);

    // around execution any public method
    @Around("execution(public * com.infotech.isg.service..*.*(..))"
            + " || execution(public * com.infotech.isg.repository..*.*(..))"
            + " || execution(public * com.infotech.isg.validation..*.*(..))"
            + " || execution(public * com.infotech.isg.proxy..*.*(..))")
    public Object logMethodEntry(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        LOG.debug("{}.{}({}): {} in {} msec", joinPoint.getSignature().getDeclaringType().getSimpleName(),
                  joinPoint.getSignature().getName(),
                  joinPoint.getArgs(),
                  result,
                  System.currentTimeMillis() - start);
        return result;
    }
}
