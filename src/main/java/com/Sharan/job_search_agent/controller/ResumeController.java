package com.Sharan.job_search_agent.controller;

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
                    .body(Map.of("error", "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (!lower.endsWith(".pdf") && !lower.endsWith(".docx") && !lower.endsWith(".doc")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only PDF and DOCX files are supported"));
            }
        }

        log.info("Resume upload request — userId: {} | file: {}", userId, filename);

        ResumeDocument doc = resumeService.uploadAndProcess(userId, file);

        return ResponseEntity.ok(Map.of(
                "message", "Resume uploaded and processed successfully",
                "documentId", doc.getId(),
                "filename", doc.getOriginalFilename(),
                "extractedSkills", doc.getExtractedSkills(),
                "skillCount", doc.getExtractedSkills() != null
                        ? doc.getExtractedSkills().length : 0
        ));
    }


    @PostMapping("/analyze/{jobId}")
    public ResponseEntity<?> analyzeResume(
            @PathVariable UUID jobId,
            @RequestParam("userId") String userId) {

        log.info("Resume analysis request — userId: {} | jobId: {}", userId, jobId);

        ResumeAnalysis analysis = resumeAnalysisService.analyzeResumeForJob(userId, jobId);

        return ResponseEntity.ok(Map.of(
                "userId", analysis.getUserId(),
                "jobId", jobId,
                "matchScore", analysis.getMatchScore(),
                "missingSkills", analysis.getMissingSkills() != null
                        ? analysis.getMissingSkills() : new String[0],
                "atsFeedback", analysis.getAtsFeedback() != null
                        ? analysis.getAtsFeedback() : new String[0],
                "suggestedImprovements", analysis.getSuggestedImprovements() != null
                        ? analysis.getSuggestedImprovements() : new String[0],
                "resumeFeedback", analysis.getResumeFeedback() != null
                        ? analysis.getResumeFeedback() : ""
        ));
    }


    @GetMapping("/analysis/{jobId}")
    public ResponseEntity<?> getStoredAnalysis(
            @PathVariable UUID jobId,
            @RequestParam("userId") String userId) {

        return resumeAnalysisService.getStoredAnalysis(userId, jobId)
                .map(analysis -> ResponseEntity.ok(Map.of(
                        "userId", analysis.getUserId(),
                        "jobId", jobId,
                        "matchScore", analysis.getMatchScore(),
                        "missingSkills", analysis.getMissingSkills() != null
                                ? analysis.getMissingSkills() : new String[0],
                        "atsFeedback", analysis.getAtsFeedback() != null
                                ? analysis.getAtsFeedback() : new String[0],
                        "suggestedImprovements", analysis.getSuggestedImprovements() != null
                                ? analysis.getSuggestedImprovements() : new String[0],
                        "resumeFeedback", analysis.getResumeFeedback() != null
                                ? analysis.getResumeFeedback() : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}