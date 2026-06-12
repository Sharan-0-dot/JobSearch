package com.Sharan.job_search_agent.repository;


import com.Sharan.job_search_agent.model.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, UUID> {

    Optional<JobListing> findByDedupHash(String dedupHash);

    Optional<JobListing> findByExternalId(String externalId);

    boolean existsByDedupHash(String dedupHash);

    // Paginated job listing for GET /api/jobs
    Page<JobListing> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Search jobs by location - used as fallback when embeddings not ready
    List<JobListing> findByLocationContainingIgnoreCase(String location);

    // Search jobs by title keyword
    List<JobListing> findByTitleContainingIgnoreCase(String keyword);

    // Find jobs by company name
    List<JobListing> findByCompanyIgnoreCase(String company);

    // Full text search across title + description
    // Uses Postgres ILIKE for case-insensitive matching
    @Query("""
            SELECT j FROM JobListing j
            WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY j.createdAt DESC
            """)
    List<JobListing> searchByKeyword(@Param("keyword") String keyword);

    // Find jobs by employment type - e.g. "Intern", "Full-time"
    List<JobListing> findByEmploymentTypeIgnoreCase(String employmentType);

    // Find jobs by source - e.g. "LinkedIn", "Indeed"
    List<JobListing> findBySourceIgnoreCase(String source);
}
