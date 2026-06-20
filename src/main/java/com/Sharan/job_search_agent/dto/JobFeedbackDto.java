package com.Sharan.job_search_agent.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobFeedbackDto(
        UUID id,
        String userId,
        UUID jobId,
        String jobTitle,
        String company,
        Boolean liked,
        Boolean applied,
        Boolean matched,
        String userFeedback,
        LocalDateTime createdAt
) {
    public static JobFeedbackDto from(com.Sharan.job_search_agent.model.JobFeedback fb) {
        return new JobFeedbackDto(
                fb.getId(), fb.getUserId(),
                fb.getJobListing().getId(), fb.getJobListing().getTitle(), fb.getJobListing().getCompany(),
                fb.getLiked(), fb.getApplied(), fb.getMatched(),
                fb.getUserFeedback(), fb.getCreatedAt()
        );
    }
}