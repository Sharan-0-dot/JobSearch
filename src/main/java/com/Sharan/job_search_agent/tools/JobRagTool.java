package com.Sharan.job_search_agent.tools;

import com.Sharan.job_search_agent.model.JobEmbedding;
import com.Sharan.job_search_agent.model.JobListing;
import com.Sharan.job_search_agent.model.JobRankingScore;
import com.Sharan.job_search_agent.model.UserProfile;
import com.Sharan.job_search_agent.repository.JobEmbeddingRepository;
import com.Sharan.job_search_agent.repository.UserProfileRepository;
import com.Sharan.job_search_agent.service.EmbeddingService;
import com.Sharan.job_search_agent.service.RankingService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRagTool {

    private final JobEmbeddingRepository jobEmbeddingRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmbeddingService embeddingService;
    private final RankingService rankingService;

    @Value("${agent.search.top-k:5}")
    private int topK;

    @Tool("""
    Search and rank previously cached/stored jobs in the database.
    
    Use ONLY when user asks for:
    - "Best matching jobs for me"
    - "Personalized matches"
    - "Jobs matching my skills"
    - "Rank jobs for my profile"
    
    This tool does NOT fetch new jobs - it searches the local database.
    Always call this AFTER searchLiveJobs to get cached results.
    
    Parameters:
    - userId: The user's ID
    - query: Natural language search query
    
    Return: Ranked list of jobs from the database that match the query.
    
    IMPORTANT: Call getUserProfile(userId) BEFORE calling this for personalized ranking.
    """)
    public String findMatchingJobs(
            @P("The user's unique ID") String userId,
            @P("Natural language search query for jobs (e.g., backend developer, data scientist)") String query) {
        log.info("JobRagTool invoked | userId: {} | query: {}", userId, query);

        try {
            // Step 1 — Embed the user's natural language query
            float[] queryVector = embeddingService.embedText(query);

            // Step 2 — pgvector cosine similarity search
            // Converts float[] to postgres vector string format: "[0.1, 0.2, ...]"
            String vectorString = toVectorString(queryVector);
            List<JobEmbedding> similarEmbeddings = jobEmbeddingRepository
                    .findTopKSimilarJobs(vectorString, topK);

            if (similarEmbeddings.isEmpty()) {
                return "No cached jobs found. Try searching for live jobs first.";
            }

            // Step 3 — Extract JobListing from embeddings
            List<JobListing> jobs = similarEmbeddings.stream()
                    .map(JobEmbedding::getJobListing)
                    .collect(Collectors.toList());

            // Step 4 — Apply deterministic ranking if user profile exists
            UserProfile userProfile = userProfileRepository.findByUserId(userId).orElse(null);

            if (userProfile != null) {
                List<JobRankingScore> ranked = rankingService.rankJobsForUser(userProfile, jobs);
                return buildRankedJobSummary(ranked);
            }

            // No profile? Return semantic results without ranking
            return buildJobSummaryFromListings(jobs);

        } catch (Exception e) {
            log.error("JobRagTool failed: {}", e.getMessage());
            return "Semantic job search failed: " + e.getMessage();
        }
    }


    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildRankedJobSummary(List<JobRankingScore> ranked) {
        StringBuilder sb = new StringBuilder();
        sb.append("Top ").append(ranked.size()).append(" personalized matches:\n\n");

        for (int i = 0; i < ranked.size(); i++) {
            JobRankingScore score = ranked.get(i);
            JobListing job = score.getJobListing();  // correct field name
            sb.append(i + 1).append(". ")
                    .append(job.getTitle()).append(" at ").append(job.getCompany())
                    .append("\n   Location: ").append(job.getLocation())
                    .append("\n   Match Score: ").append(score.getFinalScore()).append("/100")
                    .append("\n   Skill Overlap: ").append(score.getSkillOverlapScore())
                    .append(" | Location: ").append(score.getLocationMatch())
                    .append(" | Experience: ").append(score.getExperienceMatch())
                    .append("\n   Job ID: ").append(job.getId())
                    .append("\n\n");
        }

        return sb.toString();
    }

    private String buildJobSummaryFromListings(List<JobListing> jobs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(jobs.size()).append(" semantically similar jobs:\n\n");

        for (int i = 0; i < jobs.size(); i++) {
            JobListing job = jobs.get(i);
            sb.append(i + 1).append(". ")
                    .append(job.getTitle()).append(" at ").append(job.getCompany())
                    .append(" | ").append(job.getLocation())
                    .append(" | ID: ").append(job.getId())
                    .append("\n");
        }

        return sb.toString();
    }
}