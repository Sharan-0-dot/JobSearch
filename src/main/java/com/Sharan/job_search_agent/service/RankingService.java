package com.Sharan.job_search_agent.service;

import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.model.JobRankingScore;
import com.Sharan.job_search_agent.model.UserProfile;
import com.Sharan.job_search_agent.repository.JobRankingScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final JobRankingScoreRepository jobRankingScoreRepository;

    @Value("${agent.ranking.skill-overlap-weight:0.35}")
    private double skillOverlapWeight;

    @Value("${agent.ranking.location-weight:0.20}")
    private double locationWeight;

    @Value("${agent.ranking.experience-weight:0.15}")
    private double experienceWeight;

    @Value("${agent.ranking.tech-stack-weight:0.20}")
    private double techStackWeight;

    @Value("${agent.ranking.salary-weight:0.10}")
    private double salaryWeight;


    public List<JobRankingScore> rankJobsForUser(UserProfile userProfile, List<JobListing> jobs) {
        log.info("Ranking {} jobs for user: {}", jobs.size(), userProfile.getUserId());

        List<JobRankingScore> scores = jobs.stream()
                .map(job -> computeScore(userProfile, job))
                .sorted(Comparator.comparingDouble(
                        s -> -s.getFinalScore().doubleValue()))  // descending
                .collect(Collectors.toList());

        jobRankingScoreRepository.saveAll(scores);
        return scores;
    }


    public JobRankingScore computeScore(UserProfile userProfile, JobListing job) {
        double skillScore      = computeSkillOverlap(userProfile, job);
        double locationScore   = computeLocationMatch(userProfile, job);
        double experienceScore = computeExperienceMatch(userProfile, job);
        double techScore       = computeTechStackMatch(userProfile, job);
        double salaryScore     = computeSalaryMatch(userProfile, job);

        double rawFinal = (skillScore      * skillOverlapWeight)
                + (locationScore   * locationWeight)
                + (experienceScore * experienceWeight)
                + (techScore       * techStackWeight)
                + (salaryScore     * salaryWeight);


        BigDecimal finalScore = BigDecimal.valueOf(rawFinal * 100)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Job: {} | Final: {} (skill={}, loc={}, exp={}, tech={}, salary={})",
                job.getTitle(), finalScore,
                skillScore, locationScore, experienceScore, techScore, salaryScore);


        return JobRankingScore.builder()
                .userId(userProfile.getUserId())
                .jobListing(job)
                .skillOverlapScore(toBD(skillScore))
                .locationMatch(toBD(locationScore))
                .experienceMatch(toBD(experienceScore))
                .techStackMatch(toBD(techScore))
                .salaryMatch(toBD(salaryScore))
                .finalScore(finalScore)
                .build();
    }


    private double computeSkillOverlap(UserProfile user, JobListing job) {

        Set<String> userSkills = mergeAndNormalize(user.getSkills(), user.getExtractedSkills());
        Set<String> jobSkills  = normalizeArray(job.getSkills());

        if (jobSkills.isEmpty()) return 0.5; // neutral — job has no skill data

        long overlap = userSkills.stream().filter(jobSkills::contains).count();
        return Math.min(1.0, (double) overlap / jobSkills.size());
    }

    private double computeLocationMatch(UserProfile user, JobListing job) {

        if (Boolean.TRUE.equals(user.getPreferredRemote())) {
            if (job.getRemoteStatus() != null &&
                    job.getRemoteStatus().toLowerCase().contains("remote")) {
                return 1.0;
            }
        }

        if (user.getPreferredLocations() == null || user.getPreferredLocations().length == 0) {
            return 0.5;
        }
        if (job.getLocation() == null) return 0.3;

        String jobLoc = job.getLocation().toLowerCase();
        boolean matches = Arrays.stream(user.getPreferredLocations())
                .map(String::toLowerCase)
                .anyMatch(jobLoc::contains);

        return matches ? 1.0 : 0.2;
    }

    private double computeExperienceMatch(UserProfile user, JobListing job) {
        if (user.getExperienceYears() == null) return 0.5;

        int years = user.getExperienceYears();
        String level = job.getExperienceLevel();
        if (level == null) return 0.5;

        return switch (level.toLowerCase()) {
            case "entry", "intern", "fresher", "junior" -> years <= 2 ? 1.0 : 0.4;
            case "mid", "associate"                     -> years >= 2 && years <= 5 ? 1.0 : 0.5;
            case "senior", "lead", "staff"              -> years >= 5 ? 1.0 : 0.3;
            default                                     -> 0.5;
        };
    }

    private double computeTechStackMatch(UserProfile user, JobListing job) {
        return computeSkillOverlap(user, job);
    }

    private double computeSalaryMatch(UserProfile user, JobListing job) {
        if (user.getSalaryMin() == null || job.getSalaryMin() == null) return 0.5;

        double userMin = user.getSalaryMin().doubleValue();
        double userMax = user.getSalaryMax() != null
                ? user.getSalaryMax().doubleValue()
                : userMin * 1.5;

        double jobMin = job.getSalaryMin().doubleValue();
        double jobMax = job.getSalaryMax() != null
                ? job.getSalaryMax().doubleValue()
                : jobMin * 1.5;

        return (userMin <= jobMax && jobMin <= userMax) ? 1.0 : 0.2;
    }


    private Set<String> mergeAndNormalize(String[] a, String[] b) {
        Set<String> result = new HashSet<>();
        if (a != null) Arrays.stream(a).map(s -> s.toLowerCase().trim()).forEach(result::add);
        if (b != null) Arrays.stream(b).map(s -> s.toLowerCase().trim()).forEach(result::add);
        return result;
    }

    private Set<String> normalizeArray(String[] arr) {
        if (arr == null) return Collections.emptySet();
        return Arrays.stream(arr)
                .map(s -> s.toLowerCase().trim())
                .collect(Collectors.toSet());
    }

    private BigDecimal toBD(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Vector length mismatch");
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}