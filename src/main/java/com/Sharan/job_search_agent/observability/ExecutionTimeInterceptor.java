package com.Sharan.job_search_agent.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ExecutionTimeInterceptor {


    private final MeterRegistry meterRegistry;


    @Pointcut("@annotation(dev.langchain4j.agent.tool.Tool)")
    public void toolMethodPointcut() {

    }


    @Around("toolMethodPointcut()")
    public Object measureToolExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String toolName = className + "." + methodName;

        log.debug("Tool invocation started: {}", toolName);

        Timer.Sample sample = Timer.start(meterRegistry);

        boolean success = true;

        try {
            Object result = joinPoint.proceed();

            log.debug("Tool invocation completed: {}", toolName);
            return result;

        } catch (Throwable ex) {
            success = false;
            log.error("Tool invocation failed: {} | error: {}", toolName, ex.getMessage());
            throw ex;

        } finally {

            String outcome = success ? "success" : "failure";
            sample.stop(Timer.builder("job_search.tool.execution")
                    .description("Time taken to execute agent tools")
                    .tag("tool", className)
                    .tag("method", methodName)
                    .tag("outcome", outcome)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));

            log.info("Tool [{}] completed in outcome={}", toolName, outcome);
        }
    }
}