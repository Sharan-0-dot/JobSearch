package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.JobEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobEmbeddingRepository extends JpaRepository<JobEmbedding, UUID> {


    Optional<JobEmbedding> findByJobListing_Id(UUID jobId);

    boolean existsByJobListing_Id(UUID jobId);


    @Query(value = """
            SELECT je.* FROM job_embeddings je
            ORDER BY je.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<JobEmbedding> findTopKSimilarJobs(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK
    );


    @Query(value = """
            SELECT je.* FROM job_embeddings je
            JOIN job_listings jl ON je.job_id = jl.id
            WHERE LOWER(jl.location) LIKE LOWER(CONCAT('%', :location, '%'))
            ORDER BY je.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<JobEmbedding> findTopKSimilarJobsByLocation(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK,
            @Param("location") String location
    );


    @Query(value = """
            SELECT je.* FROM job_embeddings je
            JOIN job_listings jl ON je.job_id = jl.id
            WHERE LOWER(jl.employment_type) LIKE LOWER(CONCAT('%', :employmentType, '%'))
            ORDER BY je.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<JobEmbedding> findTopKSimilarJobsByType(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK,
            @Param("employmentType") String employmentType
    );


    @Query(value = """
            SELECT je.* FROM job_embeddings je
            JOIN job_listings jl ON je.job_id = jl.id
            WHERE LOWER(jl.location) LIKE LOWER(CONCAT('%', :location, '%'))
            AND LOWER(jl.employment_type) LIKE LOWER(CONCAT('%', :employmentType, '%'))
            ORDER BY je.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<JobEmbedding> findTopKSimilarJobsByLocationAndType(
            @Param("queryVector") String queryVector,
            @Param("topK") int topK,
            @Param("location") String location,
            @Param("employmentType") String employmentType
    );

    @Query("SELECT COUNT(je) FROM JobEmbedding je")
    long countEmbeddedJobs();
}
