package com.Sharan.job_search_agent.dto;

import java.util.UUID;

public record ResumeUploadResponse(
        String message,
        UUID documentId,
        String filename,
        String[] extractedSkills,
        int skillCount
) {}