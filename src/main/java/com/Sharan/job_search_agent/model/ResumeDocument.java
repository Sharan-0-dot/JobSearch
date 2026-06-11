package com.Sharan.job_search_agent.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resume_documents", indexes = {
        @Index(name = "idx_resume_doc_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Which user uploaded this resume
    // String FK to user_profiles.user_id (not UUID FK - keeps it simple)
    @Column(name = "user_id", nullable = false)
    private String userId;

    // Original filename as uploaded - "Sharan_Resume_2025.pdf"
    @Column(name = "original_filename", columnDefinition = "TEXT")
    private String originalFilename;

    // Where the file is stored on disk
    // e.g. "uploads/resumes/sharan-001/Sharan_Resume_2025.pdf"
    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    // Full text extracted by Apache Tika from the PDF/DOCX
    // This is the raw text before any AI processing
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    // What skills AI found in this specific resume upload
    // Stored per-document so you can compare across versions
    @Column(name = "extracted_skills", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] extractedSkills;

    // Projects mentioned in resume
    // e.g. ["Built Kafka consumer", "Designed PostgreSQL schema for 10M records"]
    @Column(name = "extracted_projects", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] extractedProjects;

    // Education entries found in resume
    // e.g. ["B.Tech CSE, VTU, 2026"]
    @Column(name = "extracted_education", columnDefinition = "TEXT[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] extractedEducation;

    // When this resume was uploaded
    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
