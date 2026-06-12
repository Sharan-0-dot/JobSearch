package com.Sharan.job_search_agent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_feedback", indexes = {
        @Index(name = "idx_job_feedback_user", columnList = "user_id"),
        @Index(name = "idx_job_feedback_user_job", columnList = "user_id, job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobListing jobListing;

    @Column(name = "liked")
    private Boolean liked;

    @Column(name = "applied")
    private Boolean applied;

    @Column(name = "matched")
    private Boolean matched;

    @Column(name = "user_feedback", columnDefinition = "TEXT")
    private String userFeedback;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}