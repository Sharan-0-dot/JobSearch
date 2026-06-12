package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.JobRankingScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRankingScoreRepository extends JpaRepository<JobRankingScore, UUID> {

    Optional<JobRankingScore> findByUserIdAndJobListing_Id(
            String userId,
            UUID jobId
    );

    boolean existsByUserIdAndJobListing_Id(String userId, UUID jobId);

    List<JobRankingScore> findTop5ByUserIdOrderByFinalScoreDesc(String userId);

    @Query("""
            SELECT r FROM JobRankingScore r
            WHERE r.userId = :userId
            ORDER BY r.finalScore DESC
            """)
    List<JobRankingScore> findAllByUserIdOrderByScoreDesc(
            @Param("userId") String userId
    );

    @Modifying
    @Transactional
    void deleteByUserId(String userId);

    @Modifying
    @Transactional
    void deleteByUserIdAndJobListing_Id(String userId, UUID jobId);

    @Query("""
            SELECT AVG(r.finalScore)
            FROM JobRankingScore r
            WHERE r.userId = :userId
            """)
    Double findAverageScoreByUserId(@Param("userId") String userId);
}
