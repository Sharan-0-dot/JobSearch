package com.Sharan.job_search_agent.service;

import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.model.ResumeAnalysis;
import com.Sharan.job_search_agent.model.UserProfile;
import com.Sharan.job_search_agent.repository.JobListingRepository;
import com.Sharan.job_search_agent.repository.ResumeAnalysisRepository;
import com.Sharan.job_search_agent.repository.UserProfileRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

    private final UserProfileRepository userProfileRepository;
    private final JobListingRepository jobListingRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ChatLanguageModel chatLanguageModel;


    @Transactional
    public ResumeAnalysis analyzeResumeForJob(String userId, UUID jobId) {
        log.info("Analyzing resume for user: {} against job: {}", userId, jobId);

        UserProfile user = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

        JobListing job = jobListingRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        // Step 1 — Deterministic: compute match score + find missing skills
        Set<String> userSkills   = mergeUserSkills(user);
        Set<String> jobSkills    = normalizeArray(job.getSkills());
        Set<String> missingSkills = computeMissingSkills(userSkills, jobSkills);
        BigDecimal matchScore     = computeMatchScore(userSkills, jobSkills);

        log.debug("Match score: {} | Missing skills: {}", matchScore, missingSkills);

        // Step 2 — LLM: generate personalized coaching feedback
        String[] atsFeedback          = generateAtsFeedback(user, job, missingSkills);
        String[] suggestedImprovements = generateSuggestedImprovements(missingSkills);
        String resumeFeedback          = generateResumeFeedback(user, job, missingSkills);

        // Step 3 — Persist analysis for feedback loop
        ResumeAnalysis analysis = ResumeAnalysis.builder()
                .userId(userId)
                .jobListing(job)
                .matchScore(matchScore)
                .missingSkills(missingSkills.toArray(new String[0]))
                .atsFeedback(atsFeedback)
                .suggestedImprovements(suggestedImprovements)
                .resumeFeedback(resumeFeedback)
                .build();

        resumeAnalysisRepository.save(analysis);
        log.info("Resume analysis saved for user: {} job: {}", userId, jobId);

        return analysis;
    }


    public Optional<ResumeAnalysis> getStoredAnalysis(String userId, UUID jobId) {
        return resumeAnalysisRepository.findByUserIdAndJobListing_Id(userId, jobId);
    }

    private Set<String> computeMissingSkills(Set<String> userSkills, Set<String> jobSkills) {
        return jobSkills.stream()
                .filter(skill -> !userSkills.contains(skill))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private BigDecimal computeMatchScore(Set<String> userSkills, Set<String> jobSkills) {
        if (jobSkills.isEmpty()) return BigDecimal.valueOf(50); // neutral

        long matched = userSkills.stream().filter(jobSkills::contains).count();
        double ratio = (double) matched / jobSkills.size();

        return BigDecimal.valueOf(ratio * 100).setScale(2, RoundingMode.HALF_UP);
    }

    private String[] generateAtsFeedback(UserProfile user, JobListing job, Set<String> missingSkills) {
        try {
            String prompt = String.format("""
                    You are an ATS (Applicant Tracking System) expert.
                    
                    Job Title: %s
                    Company: %s
                    Job Description (first 1000 chars): %s
                    Missing Skills: %s
                    
                    Give 3-5 specific ATS keyword suggestions to add to the resume.
                    Return ONLY a numbered list, one suggestion per line.
                    Each suggestion must be under 15 words.
                    No explanations outside the list.
                    """,
                    job.getTitle(),
                    job.getCompany(),
                    truncate(job.getDescription(), 1000),
                    String.join(", ", missingSkills)
            );

            String response = chatLanguageModel.generate(prompt);
            return parseNumberedList(response);

        } catch (Exception e) {
            log.warn("ATS feedback generation failed: {}", e.getMessage());
            return new String[]{"Review job description keywords and add relevant ones to your resume"};
        }
    }

    private String[] generateSuggestedImprovements(Set<String> missingSkills) {
        if (missingSkills.isEmpty()) return new String[]{"Great match — no critical gaps found"};

        try {
            String prompt = String.format("""
                    A candidate is missing these skills: %s
                    
                    Suggest 2-3 concrete project ideas to learn and demonstrate these skills.
                    Return ONLY a numbered list, one project per line.
                    Each suggestion must be specific and actionable, under 20 words.
                    No explanations outside the list.
                    """,
                    String.join(", ", missingSkills)
            );

            String response = chatLanguageModel.generate(prompt);
            return parseNumberedList(response);

        } catch (Exception e) {
            log.warn("Improvement suggestions failed: {}", e.getMessage());
            return missingSkills.stream()
                    .map(s -> "Build a project demonstrating " + s)
                    .toArray(String[]::new);
        }
    }

    private String generateResumeFeedback(UserProfile user, JobListing job, Set<String> missingSkills) {
        try {
            String prompt = String.format("""
                    You are an expert career coach. Write a short 2-3 sentence personalized 
                    feedback for a candidate applying to this role.
                    
                    Candidate skills: %s
                    Job title: %s
                    Missing skills: %s
                    
                    Be encouraging but honest. Focus on what to highlight and what to improve.
                    Return ONLY the feedback paragraph, no headers or bullet points.
                    """,
                    String.join(", ", mergeUserSkills(user)),
                    job.getTitle(),
                    String.join(", ", missingSkills)
            );

            return chatLanguageModel.generate(prompt).trim();

        } catch (Exception e) {
            log.warn("Resume feedback generation failed: {}", e.getMessage());
            return "Focus on highlighting your strongest skills and addressing the identified gaps.";
        }
    }

    private Set<String> mergeUserSkills(UserProfile user) {
        Set<String> skills = new HashSet<>();
        if (user.getSkills() != null)
            Arrays.stream(user.getSkills()).map(s -> s.toLowerCase().trim()).forEach(skills::add);
        if (user.getExtractedSkills() != null)
            Arrays.stream(user.getExtractedSkills()).map(s -> s.toLowerCase().trim()).forEach(skills::add);
        return skills;
    }

    private Set<String> normalizeArray(String[] arr) {
        if (arr == null) return Collections.emptySet();
        return Arrays.stream(arr)
                .map(s -> s.toLowerCase().trim())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        return text.length() > maxChars ? text.substring(0, maxChars) : text;
    }

    private String[] parseNumberedList(String response) {
        if (response == null || response.isBlank()) return new String[0];

        return Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.replaceAll("^[0-9]+[.)\\-]\\s*", "")) // remove "1. " or "1) "
                .filter(line -> !line.isBlank())
                .toArray(String[]::new);
    }
}
