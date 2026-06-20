package com.Sharan.job_search_agent.dto;

import java.util.UUID;

public record FeedbackRequest(
        String userId,
        UUID jobId,
        Boolean liked,
        Boolean applied,
        Boolean matched,
        String feedback
) {}