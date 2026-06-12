package com.Sharan.job_search_agent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_ranking_score", indexes = {
        @Index(name = "idx_ranking_score_user", columnList = "user_id"),
        @Index(name = "idx_ranking_score_user_job", columnList = "user_id, job_id"),
        @Index(name = "idx_ranking_score_final", columnList = "user_id, final_score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRankingScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobListing jobListing;

    @Column(name = "skill_overlap_score", precision = 5, scale = 4)
    private BigDecimal skillOverlapScore;

    @Column(name = "location_match", precision = 5, scale = 4)
    private BigDecimal locationMatch;

    @Column(name = "experience_match", precision = 5, scale = 4)
    private BigDecimal experienceMatch;

    @Column(name = "tech_stack_match", precision = 5, scale = 4)
    private BigDecimal techStackMatch;

    @Column(name = "salary_match", precision = 5, scale = 4)
    private BigDecimal salaryMatch;

    private BigDecimal finalScore;

    @CreationTimestamp
    @Column(name = "computed_at", updatable = false)
    private LocalDateTime computedAt;
}