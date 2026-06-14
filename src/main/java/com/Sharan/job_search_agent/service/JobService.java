package com.Sharan.job_search_agent.service;

import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobListingRepository jobListingRepository;
    private final EmbeddingService embeddingService;

    @Transactional
    public JobListing saveIfNotDuplicate(JobListing job) {
        String hash = computeDedupHash(job.getTitle(), job.getCompany(), job.getLocation());
        job.setDedupHash(hash);

        Optional<JobListing> existing = jobListingRepository.findByDedupHash(hash);
        if (existing.isPresent()) {
            log.debug("Duplicate job skipped: {} at {}", job.getTitle(), job.getCompany());
            return existing.get();
        }

        job.setFetchedAt(LocalDateTime.now());
        JobListing saved = jobListingRepository.save(job);

        embeddingService.embedAndSaveJob(saved);

        log.info("Saved new job: {} at {}", saved.getTitle(), saved.getCompany());
        return saved;
    }


    @Transactional
    public List<JobListing> saveAllIfNotDuplicate(List<JobListing> jobs) {
        List<JobListing> saved = new ArrayList<>();
        for (JobListing job : jobs) {
            JobListing result = saveIfNotDuplicate(job);
            saved.add(result);
        }
        log.info("Batch save complete: {}/{} jobs processed", saved.size(), jobs.size());
        return saved;
    }

    public JobListing mapFromJSearchResponse(Map<String, Object> raw) {
        return JobListing.builder()
                .externalId(getString(raw, "job_id"))
                .title(getString(raw, "job_title"))
                .company(getString(raw, "employer_name"))
                .location(buildLocation(raw))
                .employmentType(getString(raw, "job_employment_type"))
                .experienceLevel(getString(raw, "job_required_experience_in_months") != null
                        ? inferExperienceLevel(getString(raw, "job_required_experience_in_months"))
                        : "Entry")
                .remoteStatus(Boolean.TRUE.equals(raw.get("job_is_remote")) ? "Fully Remote" : "On-site")
                .description(getString(raw, "job_description"))
                .applyLink(getString(raw, "job_apply_link"))
                .source(getString(raw, "job_publisher"))
                .companyLogoUrl(getString(raw, "employer_logo"))
                .skills(extractSkillsArray(raw))
                .postedAt(LocalDateTime.now())
                .build();
    }

    public String computeDedupHash(String title, String company, String location) {
        String raw = String.join("|",
                title   != null ? title.toLowerCase().trim()   : "",
                company != null ? company.toLowerCase().trim() : "",
                location != null ? location.toLowerCase().trim() : "");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private String buildLocation(Map<String, Object> raw) {
        String city    = getString(raw, "job_city");
        String country = getString(raw, "job_country");
        if (city != null && country != null) return city + ", " + country;
        if (city != null) return city;
        if (country != null) return country;
        return "Unknown";
    }

    private String inferExperienceLevel(String monthsStr) {
        try {
            int months = Integer.parseInt(monthsStr);
            if (months <= 12) return "Entry";
            if (months <= 36) return "Mid";
            return "Senior";
        } catch (NumberFormatException e) {
            return "Entry";
        }
    }

    @SuppressWarnings("unchecked")
    private String[] extractSkillsArray(Map<String, Object> raw) {
        Object highlights = raw.get("job_highlights");
        if (highlights instanceof Map) {
            Map<String, Object> highlightMap = (Map<String, Object>) highlights;
            Object quals = highlightMap.get("Qualifications");
            if (quals instanceof List) {
                return ((List<String>) quals).toArray(new String[0]);
            }
        }
        return new String[0];
    }
}