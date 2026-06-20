package com.Sharan.job_search_agent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record JobListingDto(
        UUID id,
        String title,
        String company,
        String location,
        String employmentType,
        String experienceLevel,
        String remoteStatus,
        String jobType,
        String description,
        String[] skills,
        String applyLink,
        Boolean isApplyLinkValid,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String source,
        String companyLogoUrl,
        LocalDateTime postedAt
) {
    public static JobListingDto from(com.Sharan.job_search_agent.model.JobListing job) {
        return new JobListingDto(
                job.getId(), job.getTitle(), job.getCompany(), job.getLocation(),
                job.getEmploymentType(), job.getExperienceLevel(), job.getRemoteStatus(),
                job.getJobType(), job.getDescription(), job.getSkills(), job.getApplyLink(),
                job.getIsApplyLinkValid(), job.getSalaryMin(), job.getSalaryMax(),
                job.getSalaryCurrency(), job.getSource(), job.getCompanyLogoUrl(),
                job.getPostedAt()
        );
    }
}