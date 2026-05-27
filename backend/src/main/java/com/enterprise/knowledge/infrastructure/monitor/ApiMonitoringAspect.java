package com.enterprise.knowledge.infrastructure.monitor;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * API监控切面 - 自动记录所有API调用
 */
@Aspect
@Component
public class ApiMonitoringAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiMonitoringAspect.class);

    private final SystemMonitor systemMonitor;

    public ApiMonitoringAspect(SystemMonitor systemMonitor) {
        this.systemMonitor = systemMonitor;
    }

    @Pointcut("within(com.enterprise.knowledge.api.rest..*)")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object monitorApi(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String endpoint = getEndpointName(joinPoint);
        boolean success = true;
        
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            logger.error("API调用失败: {}", endpoint, e);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            systemMonitor.recordApiCall(endpoint, duration, success);
            logger.debug("API调用: {} 耗时 {}ms 成功: {}", endpoint, duration, success);
        }
    }

    private String getEndpointName(ProceedingJoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getMethod() + " " + request.getRequestURI();
            }
        } catch (Exception e) {
            // 忽略异常，使用默认命名
        }
        
        return joinPoint.getSignature().toShortString();
    }
}
