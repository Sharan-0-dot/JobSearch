package com.Sharan.job_search_agent.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;

@Slf4j
@Configuration
public class ResilienceConfig {


    @Bean
    public CircuitBreaker jSearchCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(IOException.class, RuntimeException.class, ConnectException.class)
                .build();

        CircuitBreaker cb = CircuitBreakerRegistry.ofDefaults()
                .circuitBreaker("jsearch-api", config);

        cb.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit Breaker state changed: {}", event.getStateTransition()));

        return cb;
    }


    @Bean
    public Retry jSearchRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(IOException.class, ConnectException.class, RuntimeException.class)
                .build();

        Retry retry = RetryRegistry.ofDefaults()
                .retry("jsearch-api", config);

        retry.getEventPublisher()
                .onRetry(event ->
                        log.warn("Retrying JSearch API call — attempt: {}",
                                event.getNumberOfRetryAttempts()));

        return retry;
    }
}
