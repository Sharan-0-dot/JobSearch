package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.JobFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobFeedbackRepository extends JpaRepository<JobFeedback, UUID> {


    List<JobFeedback> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<JobFeedback> findByUserIdAndJobListing_Id(
            String userId,
            UUID jobId
    );

    boolean existsByUserIdAndJobListing_Id(String userId, UUID jobId);

    @Query("""
            SELECT f FROM JobFeedback f
            WHERE f.userId = :userId
            AND f.liked = true
            ORDER BY f.createdAt DESC
            """)
    List<JobFeedback> findLikedJobsByUserId(@Param("userId") String userId);

    @Query("""
            SELECT f FROM JobFeedback f
            WHERE f.userId = :userId
            AND f.applied = true
            ORDER BY f.createdAt DESC
            """)
    List<JobFeedback> findAppliedJobsByUserId(@Param("userId") String userId);

    @Query("""
            SELECT f FROM JobFeedback f
            WHERE f.userId = :userId
            AND f.liked = false
            ORDER BY f.createdAt DESC
            """)
    List<JobFeedback> findDislikedJobsByUserId(@Param("userId") String userId);

    @Query("""
            SELECT COUNT(f) FROM JobFeedback f
            WHERE f.userId = :userId
            AND f.applied = true
            """)
    long countAppliedJobsByUserId(@Param("userId") String userId);
}
