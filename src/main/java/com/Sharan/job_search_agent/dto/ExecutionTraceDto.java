package com.Sharan.job_search_agent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExecutionTraceDto(
        UUID id,
        String userId,
        String query,
        PlanningPhase planningPhase,
        String[] toolsUsed,
        Observations observations,
        String finalResponse,
        Integer executionTimeMs,
        LocalDateTime createdAt
) {
    public record PlanningPhase(String query, List<String> toolsPlanned, String reasoning) {}
    public record Observations(List<String> toolsInvoked, int responseLength) {}
}