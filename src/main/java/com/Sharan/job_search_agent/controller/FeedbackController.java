package com.Sharan.job_search_agent.controller;

import com.Sharan.job_search_agent.model.JobFeedback;
import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.repository.JobFeedbackRepository;
import com.Sharan.job_search_agent.repository.JobListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final JobFeedbackRepository jobFeedbackRepository;
    private final JobListingRepository jobListingRepository;

    @PostMapping
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");
        String jobIdStr = (String) request.get("jobId");

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
        }
        if (jobIdStr == null || jobIdStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "jobId is required"));
        }

        UUID jobId;
        try {
            jobId = UUID.fromString(jobIdStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "jobId is not a valid UUID"));
        }

        Optional<JobListing> jobOpt = jobListingRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Job not found: " + jobId));
        }

        Optional<JobFeedback> existingOpt =
                jobFeedbackRepository.findByUserIdAndJobListing_Id(userId, jobId);

        JobFeedback feedback = existingOpt.orElseGet(() -> JobFeedback.builder()
                .userId(userId)
                .jobListing(jobOpt.get())
                .build());


        if (request.containsKey("liked")) {
            feedback.setLiked((Boolean) request.get("liked"));
        }
        if (request.containsKey("applied")) {
            feedback.setApplied((Boolean) request.get("applied"));
        }
        if (request.containsKey("matched")) {
            feedback.setMatched((Boolean) request.get("matched"));
        }
        if (request.containsKey("feedback")) {
            feedback.setUserFeedback((String) request.get("feedback"));
        }

        JobFeedback saved = jobFeedbackRepository.save(feedback);

        log.info("Feedback recorded | userId: {} | jobId: {} | liked: {} | applied: {}",
                userId, jobId, saved.getLiked(), saved.getApplied());

        return ResponseEntity.ok(Map.of(
                "message", "Feedback recorded",
                "feedback", saved
        ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<JobFeedback>> getFeedbackByUser(@PathVariable String userId) {
        return ResponseEntity.ok(
                jobFeedbackRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/{userId}/liked")
    public ResponseEntity<List<JobFeedback>> getLikedJobs(@PathVariable String userId) {
        return ResponseEntity.ok(jobFeedbackRepository.findLikedJobsByUserId(userId));
    }

    @GetMapping("/{userId}/applied")
    public ResponseEntity<List<JobFeedback>> getAppliedJobs(@PathVariable String userId) {
        return ResponseEntity.ok(jobFeedbackRepository.findAppliedJobsByUserId(userId));
    }
}