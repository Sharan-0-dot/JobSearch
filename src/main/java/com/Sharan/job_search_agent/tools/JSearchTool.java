package com.Sharan.job_search_agent.tools;

import com.Sharan.job_search_agent.client.JSearchApiClient;
import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.service.JobService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
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

    @Tool("""
        ALWAYS call this tool for ANY user request about finding, searching, or looking for jobs.
        
        THIS IS THE PRIMARY AND REQUIRED TOOL FOR ALL JOB SEARCHES.
        
        Use this tool when user mentions:
        - "Find me jobs"
        - "Look for jobs"
        - "Search for jobs"
        - "Backend developer jobs"
        - "Positions in bangalore"
        - "Recently posted jobs"
        - Any role, location, or job type
        
        This tool fetches REAL-TIME job listings directly from the live JSearch API.
        It is the ONLY source of fresh, real job data.
        
        Parameters:
        - query: The job role/title (e.g., "backend developer", "software engineer", "data scientist")
        - location: The location/city (e.g., "bangalore", "mumbai", "delhi")
        - jobType: Employment type - use "FULLTIME", "PARTTIME", or "INTERN"
        
        Return: List of real, verified job listings with company, title, location, and job ID.
        
        IMPORTANT: ALWAYS extract location and role from the user's message.
        Example: "Find backend developer jobs in bangalore" → searchLiveJobs("backend developer", "bangalore", "FULLTIME")
        """)
    public String searchLiveJobs(
            @P("The job role or title to search for (e.g., backend developer, software engineer)") String query,
            @P("The location or city to search in (e.g., bangalore, mumbai, delhi)") String location,
            @P("Employment type: FULLTIME, PARTTIME, or INTERN") String jobType) {
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
