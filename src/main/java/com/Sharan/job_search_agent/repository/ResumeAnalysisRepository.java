package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, UUID> {


    List<ResumeAnalysis> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<ResumeAnalysis> findByUserIdAndJobListing_Id(
            String userId,
            UUID jobId
    );

    boolean existsByUserIdAndJobListing_Id(String userId, UUID jobId);

    @Query("""
            SELECT r FROM ResumeAnalysis r
            WHERE r.userId = :userId
            ORDER BY r.matchScore DESC
            """)
    List<ResumeAnalysis> findTopMatchesByUserId(@Param("userId") String userId);

    @Query("""
            SELECT AVG(r.matchScore)
            FROM ResumeAnalysis r
            WHERE r.userId = :userId
            """)
    Double findAverageMatchScoreByUserId(@Param("userId") String userId);
}
