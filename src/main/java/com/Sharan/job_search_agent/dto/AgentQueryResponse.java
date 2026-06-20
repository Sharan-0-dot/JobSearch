package com.Sharan.job_search_agent.dto;

import java.util.List;

public record AgentQueryResponse(
        String userId,
        String query,
        String response,
        List<String> toolsUsed,
        Long executionTimeMs,
        String warning
) {
    public static AgentQueryResponse success(String userId, String query, String response,
                                             List<String> toolsUsed, long executionTimeMs) {
        return new AgentQueryResponse(userId, query, response, toolsUsed, executionTimeMs, null);
    }

    public static AgentQueryResponse guardrailTriggered(String userId, String query,
                                                        List<String> toolsUsed) {
        return new AgentQueryResponse(
                userId, query,
                "I couldn't verify any real job listings for this request. Please provide a role, location, or job type and try again.",
                toolsUsed, null, "No job search tool was executed."
        );
    }
}