package com.Sharan.job_search_agent.tools;

import com.Sharan.job_search_agent.client.JSearchApiClient;
import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.service.JobService;
import dev.langchain4j.agent.tool.Tool;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JSearchTool {

    private final JSearchApiClient jSearchApiClient;
    private final JobService jobService;
    private final CircuitBreaker jSearchCircuitBreaker;
    private final Retry jSearchRetry;


    @Tool("Search for live job listings from JSearch API. " +
            "Use this when user wants fresh, real-time job listings. " +
            "Input: job role/query, location, job type (INTERN/FULLTIME/PARTTIME).")
    public String searchLiveJobs(String query, String location, String jobType) {
        log.info("JSearchTool invoked | query: {} | location: {} | type: {}",
                query, location, jobType);

        try {
            Supplier<List<Map<String, Object>>> apiCall = Retry.decorateSupplier(
                    jSearchRetry,
                    CircuitBreaker.decorateSupplier(
                            jSearchCircuitBreaker,
                            () -> jSearchApiClient.searchJobs(query, location, jobType, 2)
                    )
            );

            List<Map<String, Object>> rawJobs = apiCall.get();

            if (rawJobs.isEmpty()) {
                return "No jobs found for query: " + query + " in " + location;
            }

            List<JobListing> jobs = rawJobs.stream()
                    .map(jobService::mapFromJSearchResponse)
                    .collect(Collectors.toList());

            List<JobListing> saved = jobService.saveAllIfNotDuplicate(jobs);

            return buildJobSummary(saved);

        } catch (Exception e) {
            log.error("JSearchTool failed: {}", e.getMessage());
            return "Live job search is temporarily unavailable. " +
                    "Please try the cached job search instead.";
        }
    }


    private String buildJobSummary(List<JobListing> jobs) {
        if (jobs.isEmpty()) return "No new jobs found.";

        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(jobs.size()).append(" jobs:\n\n");

        for (int i = 0; i < Math.min(jobs.size(), 10); i++) {
            JobListing job = jobs.get(i);
            sb.append(i + 1).append(". ")
                    .append(job.getTitle()).append(" at ").append(job.getCompany())
                    .append(" | Location: ").append(job.getLocation())
                    .append(" | Type: ").append(job.getEmploymentType())
                    .append(" | Remote: ").append(job.getRemoteStatus())
                    .append(" | ID: ").append(job.getId())
                    .append("\n");
        }

        return sb.toString();
    }
}
