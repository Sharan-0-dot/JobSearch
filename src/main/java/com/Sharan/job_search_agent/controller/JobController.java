package com.Sharan.job_search_agent.controller;

import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobListingRepository jobListingRepository;

    @GetMapping
    public ResponseEntity<Page<JobListing>> getAllJobs(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<JobListing> jobs = jobListingRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable UUID id) {
        return jobListingRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobListing>> searchJobs(
            @RequestParam String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        List<JobListing> results = jobListingRepository.searchByKeyword(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/location")
    public ResponseEntity<List<JobListing>> getJobsByLocation(
            @RequestParam String location) {

        List<JobListing> results =
                jobListingRepository.findByLocationContainingIgnoreCase(location);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/company")
    public ResponseEntity<List<JobListing>> getJobsByCompany(
            @RequestParam String name) {

        List<JobListing> results = jobListingRepository.findByCompanyIgnoreCase(name);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/type")
    public ResponseEntity<List<JobListing>> getJobsByType(
            @RequestParam String employmentType) {

        List<JobListing> results =
                jobListingRepository.findByEmploymentTypeIgnoreCase(employmentType);
        return ResponseEntity.ok(results);
    }
}