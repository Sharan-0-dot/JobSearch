package com.Sharan.job_search_agent.controller;

import com.Sharan.job_search_agent.dto.ErrorResponse;
import com.Sharan.job_search_agent.dto.ResumeAnalysisDto;
import com.Sharan.job_search_agent.dto.ResumeUploadResponse;
import com.Sharan.job_search_agent.model.ResumeAnalysis;
import com.Sharan.job_search_agent.model.ResumeDocument;
import com.Sharan.job_search_agent.service.ResumeAnalysisService;
import com.Sharan.job_search_agent.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeAnalysisService resumeAnalysisService;


    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("userId") String userId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("Bad request", "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (!lower.endsWith(".pdf") && !lower.endsWith(".docx") && !lower.endsWith(".doc")) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("Bad request", "Only PDF and DOCX files are supported"));
            }
        }

        log.info("Resume upload request — userId: {} | file: {}", userId, filename);

        ResumeDocument doc = resumeService.uploadAndProcess(userId, file);

        ResumeUploadResponse response = new ResumeUploadResponse(
                "Resume uploaded and processed successfully",
                doc.getId(),
                doc.getOriginalFilename(),
                doc.getExtractedSkills(),
                doc.getExtractedSkills() != null
                        ? doc.getExtractedSkills().length
                        : 0
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/analyze/{jobId}")
    public ResponseEntity<?> analyzeResume(
            @PathVariable UUID jobId,
            @RequestParam("userId") String userId) {

        log.info("Resume analysis request — userId: {} | jobId: {}", userId, jobId);

        ResumeAnalysis analysis = resumeAnalysisService.analyzeResumeForJob(userId, jobId);
        return ResponseEntity.ok(ResumeAnalysisDto.from(analysis, jobId));
    }


    @GetMapping("/analysis/{jobId}")
    public ResponseEntity<?> getStoredAnalysis(
            @PathVariable UUID jobId,
            @RequestParam("userId") String userId) {

        return resumeAnalysisService.getStoredAnalysis(userId, jobId)
                .map(a -> ResumeAnalysisDto.from(a, jobId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}