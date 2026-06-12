package com.Sharan.job_search_agent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_execution_trace", indexes = {
        @Index(name = "idx_execution_trace_user", columnList = "user_id"),
        @Index(name = "idx_execution_trace_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "query", columnDefinition = "TEXT")
    private String query;

    @Column(name = "planning_phase", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String planningPhase;

    @Column(name = "tools_used", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] toolsUsed;

    @Column(name = "observations", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String observations;

    @Column(name = "final_response", columnDefinition = "TEXT")
    private String finalResponse;

    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}