package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.ResumeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeDocumentRepository extends JpaRepository<ResumeDocument, UUID> {

    List<ResumeDocument> findByUserIdOrderByUploadedAtDesc(String userId);

    Optional<ResumeDocument> findFirstByUserIdOrderByUploadedAtDesc(String userId);

    long countByUserId(String userId);

    boolean existsByUserId(String userId);

    Optional<ResumeDocument> findByUserIdAndOriginalFilename(
            String userId,
            String originalFilename
    );

    @Query(value = """
        SELECT * FROM resume_documents
        WHERE user_id = :userId
        AND (extracted_skills IS NULL
        OR extracted_skills = '{}')
        """, nativeQuery = true)
    List<ResumeDocument> findUnprocessedByUserId(@Param("userId") String userId);
}
