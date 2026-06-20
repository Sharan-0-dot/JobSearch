package com.Sharan.job_search_agent.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ResumeAnalysisDto(
        String userId,
        UUID jobId,
        BigDecimal matchScore,
        String[] missingSkills,
        String[] atsFeedback,
        String[] suggestedImprovements,
        String resumeFeedback
) {
    public static ResumeAnalysisDto from(com.Sharan.job_search_agent.model.ResumeAnalysis a, UUID jobId) {
        return new ResumeAnalysisDto(
                a.getUserId(), jobId, a.getMatchScore(),
                a.getMissingSkills() != null ? a.getMissingSkills() : new String[0],
                a.getAtsFeedback() != null ? a.getAtsFeedback() : new String[0],
                a.getSuggestedImprovements() != null ? a.getSuggestedImprovements() : new String[0],
                a.getResumeFeedback() != null ? a.getResumeFeedback() : ""
        );
    }
}