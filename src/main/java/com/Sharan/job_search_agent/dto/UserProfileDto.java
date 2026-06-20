package com.Sharan.job_search_agent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserProfileDto(
        String userId,
        String name,
        String email,
        String[] skills,
        String[] extractedSkills,
        Integer experienceYears,
        String currentRole,
        String[] preferredRoles,
        String[] preferredLocations,
        Boolean preferredRemote,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        LocalDateTime updatedAt
) {
    public static UserProfileDto from(com.Sharan.job_search_agent.model.UserProfile p) {
        return new UserProfileDto(
                p.getUserId(), p.getName(), p.getEmail(), p.getSkills(), p.getExtractedSkills(),
                p.getExperienceYears(), p.getCurrentRole(), p.getPreferredRoles(), p.getPreferredLocations(),
                p.getPreferredRemote(), p.getSalaryMin(), p.getSalaryMax(), p.getUpdatedAt()
        );
    }
}