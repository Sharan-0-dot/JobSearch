package com.Sharan.job_search_agent.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JSearchApiClient {

    @Value("${jsearch.api.key}")
    private String apiKey;

    @Value("${jsearch.api.host}")
    private String apiHost;

    @Value("${jsearch.api.base-url}")
    private String baseUrl;

    private final WebClient webClient;


    public JSearchApiClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchJobs(
            String query,
            String location,
            String jobType,
            int numPages) {

        log.info("Calling JSearch API | query: {} | location: {} | type: {}",
                query, location, jobType);

        try {
            // Build the full query string — JSearch combines query + location
            String fullQuery = location != null && !location.isBlank()
                    ? query + " in " + location
                    : query;

            Map<String, Object> response = webClient.get()
                    .uri(baseUrl + "/search", uriBuilder -> uriBuilder
                            .queryParam("query", fullQuery)
                            .queryParam("num_pages", numPages)
                            .queryParam("job_type", jobType != null ? jobType : "")
                            .queryParam("date_posted", "all")
                            .build())
                    .header("X-RapidAPI-Key", apiKey)
                    .header("X-RapidAPI-Host", apiHost)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("data")) {
                log.warn("JSearch returned empty response");
                return Collections.emptyList();
            }

            List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("data");
            log.info("JSearch returned {} jobs", jobs.size());
            return jobs;

        } catch (WebClientResponseException e) {
            log.error("JSearch API HTTP error: {} - {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("JSearch API call failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("JSearch API unexpected error: {}", e.getMessage());
            throw new RuntimeException("JSearch API call failed: " + e.getMessage(), e);
        }
    }
}
