package com.Sharan.job_search_agent.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MetricsConfig {

    @Bean
    public Timer toolExecutionTimer(MeterRegistry registry) {
        return Timer.builder("job_search.tool.execution")
                .description("Time taken to execute agent tools (JSearch, RAG, etc.)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public Counter jsearchApiCallCounter(MeterRegistry registry) {
        return Counter.builder("job_search.jsearch.api.calls")
                .description("Total number of JSearch API calls made")
                .register(registry);
    }

    @Bean
    public Counter validationFailureCounter(MeterRegistry registry) {
        return Counter.builder("job_search.validation.failures")
                .description("Total number of input validation failures (prompt injection attempts, bad URLs)")
                .register(registry);
    }

    @Bean
    public Counter agentQueryCounter(MeterRegistry registry) {
        return Counter.builder("job_search.agent.queries")
                .description("Total number of agent queries processed")
                .register(registry);
    }
}