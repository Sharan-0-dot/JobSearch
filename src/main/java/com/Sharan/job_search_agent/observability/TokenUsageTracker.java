package com.Sharan.job_search_agent.observability;

import dev.langchain4j.model.output.TokenUsage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class TokenUsageTracker {

    private final Counter inputTokenCounter;
    private final Counter outputTokenCounter;
    private final Counter totalTokenCounter;

    public TokenUsageTracker(MeterRegistry meterRegistry) {
        this.inputTokenCounter = Counter.builder("job_search.tokens.used")
                .description("LLM tokens consumed")
                .tag("type", "input")
                .register(meterRegistry);

        this.outputTokenCounter = Counter.builder("job_search.tokens.used")
                .description("LLM tokens consumed")
                .tag("type", "output")
                .register(meterRegistry);

        this.totalTokenCounter = Counter.builder("job_search.tokens.used")
                .description("LLM tokens consumed")
                .tag("type", "total")
                .register(meterRegistry);
    }

    public void record(TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            log.debug("TokenUsage not reported by model — skipping metric recording");
            return;
        }

        int inputTokens  = tokenUsage.inputTokenCount()  != null
                ? tokenUsage.inputTokenCount()  : 0;
        int outputTokens = tokenUsage.outputTokenCount() != null
                ? tokenUsage.outputTokenCount() : 0;
        int totalTokens  = tokenUsage.totalTokenCount()  != null
                ? tokenUsage.totalTokenCount()  : 0;

        inputTokenCounter.increment(inputTokens);
        outputTokenCounter.increment(outputTokens);
        totalTokenCounter.increment(totalTokens);

        log.info("LLM token usage — input: {} | output: {} | total: {}",
                inputTokens, outputTokens, totalTokens);
    }
}