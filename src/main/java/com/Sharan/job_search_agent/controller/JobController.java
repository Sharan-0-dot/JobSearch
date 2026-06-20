package com.Sharan.job_search_agent.controller;

import com.Sharan.job_search_agent.dto.JobListingDto;
import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobListingRepository jobListingRepository;

    @GetMapping
    public ResponseEntity<?> getAllJobs(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<JobListing> jobs = jobListingRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ResponseEntity.ok(jobs.map(JobListingDto::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJobById(@PathVariable UUID id) {
        return jobListingRepository.findById(id)
                .map(JobListingDto::from)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchJobs(
            @RequestParam String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(jobListingRepository.searchByKeyword(keyword)
                .stream().map(JobListingDto::from).toList());
    }

    @GetMapping("/location")
    public ResponseEntity<?> getJobsByLocation(
            @RequestParam String location) {
        return ResponseEntity.ok(jobListingRepository.findByLocationContainingIgnoreCase(location)
                .stream().map(JobListingDto::from).toList());
    }

    @GetMapping("/company")
    public ResponseEntity<?> getJobsByCompany(
            @RequestParam String name) {
        return ResponseEntity.ok(jobListingRepository.findByCompanyIgnoreCase(name)
                .stream().map(JobListingDto::from).toList());
    }

    @GetMapping("/type")
    public ResponseEntity<?> getJobsByType(
            @RequestParam String employmentType) {
        return ResponseEntity.ok(jobListingRepository.findByEmploymentTypeIgnoreCase(employmentType)
                .stream().map(JobListingDto::from).toList());
    }
}