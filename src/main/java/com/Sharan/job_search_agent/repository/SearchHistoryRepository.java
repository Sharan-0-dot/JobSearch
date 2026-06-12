package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    Page<SearchHistory> findByUserIdOrderBySearchedAtDesc(
            String userId,
            Pageable pageable
    );

    List<SearchHistory> findTop5ByUserIdOrderBySearchedAtDesc(String userId);

    long countByUserId(String userId);

    @Query("""
            SELECT s FROM SearchHistory s
            WHERE s.userId = :userId
            AND s.resultsCount = 0
            ORDER BY s.searchedAt DESC
            """)
    List<SearchHistory> findEmptySearchesByUserId(@Param("userId") String userId);

    @Query(value = """
            SELECT query, COUNT(*) as search_count
            FROM search_history
            GROUP BY query
            ORDER BY search_count DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findMostCommonQueries();
}
