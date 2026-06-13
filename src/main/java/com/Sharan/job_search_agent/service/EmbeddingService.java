package com.Sharan.job_search_agent.service;

import com.Sharan.job_search_agent.model.JobEmbedding;
import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.model.UserProfile;
import com.Sharan.job_search_agent.repository.JobEmbeddingRepository;
import com.Sharan.job_search_agent.repository.UserProfileRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final UserProfileRepository userProfileRepository;


    public float[] embedText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Cannot embed empty text");
        }
        log.debug("Embedding text of length: {}", text.length());
        Response<Embedding> response = embeddingModel.embed(text);
        return response.content().vector();
    }

    @Async
    @Transactional
    public void embedAndSaveJob(JobListing job) {
        try {
            String jobText = buildJobText(job);
            float[] vector = embedText(jobText);

            JobEmbedding embedding = jobEmbeddingRepository
                    .findByJobListing_Id(job.getId())
                    .orElse(new JobEmbedding());

            embedding.setJobListing(job);
            embedding.setEmbedding(vector);

            jobEmbeddingRepository.save(embedding);
            log.info("Saved embedding for job: {} | {}", job.getTitle(), job.getCompany());

        } catch (Exception e) {
            log.error("Failed to embed job {}: {}", job.getId(), e.getMessage());
        }
    }


    @Transactional
    public void embedAndSaveResume(String userId, String resumeText) {
        try {
            float[] vector = embedText(resumeText);

            UserProfile profile = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("User profile not found: " + userId));

            profile.setResumeEmbedding(vector);
            userProfileRepository.save(profile);

            log.info("Saved resume embedding for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to embed resume for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Resume embedding failed", e);
        }
    }


    @Async
    public void embedJobsBatch(List<JobListing> jobs) {
        log.info("Batch embedding {} jobs", jobs.size());
        jobs.forEach(this::embedAndSaveJob);
    }


    private String buildJobText(JobListing job) {
        StringBuilder sb = new StringBuilder();

        if (job.getTitle() != null)        sb.append(job.getTitle()).append(". ");
        if (job.getCompany() != null)      sb.append(job.getCompany()).append(". ");
        if (job.getLocation() != null)     sb.append(job.getLocation()).append(". ");
        if (job.getDescription() != null)  sb.append(job.getDescription()).append(" ");
        if (job.getRequirements() != null) sb.append(job.getRequirements()).append(" ");

        // skills is String[] — use Arrays.asList or just join directly
        if (job.getSkills() != null && job.getSkills().length > 0) {
            sb.append("Skills: ")
                    .append(String.join(", ", job.getSkills()))
                    .append(".");
        }

        return sb.toString().trim();
    }
}