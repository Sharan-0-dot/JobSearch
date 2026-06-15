package com.Sharan.job_search_agent.service;

import com.Sharan.job_search_agent.model.ExecutionTrace;
import com.Sharan.job_search_agent.repository.ExecutionTraceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionTraceService {

    private final ExecutionTraceRepository executionTraceRepository;
    private final ObjectMapper objectMapper;

    public ExecutionTrace saveTrace(
            String userId,
            String query,
            String finalResponse,
            List<String> toolsUsed,
            long executionTimeMs) {

        try {

            String planningPhase = objectMapper.writeValueAsString(Map.of(
                    "query", query,
                    "toolsPlanned", toolsUsed,
                    "reasoning", "Agent selected tools based on query intent"
            ));


            String observations = objectMapper.writeValueAsString(Map.of(
                    "toolsInvoked", toolsUsed,
                    "responseLength", finalResponse != null ? finalResponse.length() : 0
            ));

            ExecutionTrace trace = ExecutionTrace.builder()
                    .userId(userId)
                    .query(query)
                    .planningPhase(planningPhase)
                    .toolsUsed(toolsUsed.toArray(new String[0]))
                    .observations(observations)
                    .finalResponse(finalResponse)
                    .executionTimeMs((int) executionTimeMs)
                    .build();

            ExecutionTrace saved = executionTraceRepository.save(trace);
            log.info("Trace saved | userId: {} | tools: {} | time: {}ms",
                    userId, toolsUsed, executionTimeMs);

            return saved;

        } catch (Exception e) {
            log.error("Failed to save execution trace: {}", e.getMessage());
            // Don't throw — tracing failure must never break the main response
            return null;
        }
    }


    public Optional<ExecutionTrace> getTrace(UUID traceId) {
        return executionTraceRepository.findById(traceId);
    }


    public Page<ExecutionTrace> getTracesByUser(String userId, Pageable pageable) {

        return executionTraceRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId,
                        pageable
                );
    }
}