package com.Sharan.job_search_agent.repository;

import com.Sharan.job_search_agent.model.ExecutionTrace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionTraceRepository extends JpaRepository<ExecutionTrace, UUID> {


    Page<ExecutionTrace> findByUserIdOrderByCreatedAtDesc(
            String userId,
            Pageable pageable
    );

    List<ExecutionTrace> findTop10ByUserIdOrderByCreatedAtDesc(String userId);

    @Query("""
            SELECT AVG(e.executionTimeMs)
            FROM ExecutionTrace e
            WHERE e.userId = :userId
            """)
    Double findAverageExecutionTimeByUserId(@Param("userId") String userId);

    @Query("""
            SELECT e FROM ExecutionTrace e
            WHERE e.executionTimeMs > :thresholdMs
            ORDER BY e.executionTimeMs DESC
            """)
    List<ExecutionTrace> findSlowExecutions(@Param("thresholdMs") int thresholdMs);

    @Query(value = """
            SELECT * FROM agent_execution_trace
            WHERE :tool = ANY(tools_used)
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<ExecutionTrace> findByToolUsed(@Param("tool") String tool);
}
