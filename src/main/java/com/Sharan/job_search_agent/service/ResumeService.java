package com.Sharan.job_search_agent.service;

import com.Sharan.job_search_agent.model.ResumeDocument;
import com.Sharan.job_search_agent.model.UserProfile;
import com.Sharan.job_search_agent.repository.ResumeDocumentRepository;
import com.Sharan.job_search_agent.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final Tika tika;
    private final ResumeDocumentRepository resumeDocumentRepository;
    private final UserProfileRepository userProfileRepository;
    private final SkillExtractionService skillExtractionService;
    private final EmbeddingService embeddingService;


    @Transactional
    public ResumeDocument uploadAndProcess(String userId, MultipartFile file) {
        log.info("Processing resume upload for user: {} | file: {}",
                userId, file.getOriginalFilename());

        // Step 1 — Parse raw text from PDF/DOCX using Apache Tika
        String extractedText = extractText(file);
        log.debug("Extracted {} characters from resume", extractedText.length());

        // Step 2 — Extract structured skills from the raw text
        String[] extractedSkills = skillExtractionService.extractSkillsFromText(extractedText);
        log.info("Extracted {} skills for user: {}", extractedSkills.length, userId);

        // Step 3 — Save resume document record
        ResumeDocument resumeDoc = ResumeDocument.builder()
                .userId(userId)
                .originalFilename(file.getOriginalFilename())
                .extractedText(extractedText)
                .extractedSkills(extractedSkills)
                .build();

        resumeDocumentRepository.save(resumeDoc);

        // Step 4 — Update user profile with resume text + extracted skills
        updateUserProfileWithResume(userId, extractedText, extractedSkills);

        // Step 5 — Generate and save resume embedding (synchronous — needed immediately)
        embeddingService.embedAndSaveResume(userId, extractedText);

        log.info("Resume processing complete for user: {}", userId);
        return resumeDoc;
    }


    public String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String text = tika.parseToString(inputStream);
            if (text == null || text.isBlank()) {
                throw new RuntimeException("Could not extract text from file — file may be empty or image-only PDF");
            }
            return text.trim();
        } catch (IOException | TikaException e) {
            log.error("Failed to parse resume file: {}", e.getMessage());
            throw new RuntimeException("Resume parsing failed: " + e.getMessage(), e);
        }
    }


    private void updateUserProfileWithResume(String userId, String resumeText, String[] extractedSkills) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(UserProfile.builder().userId(userId).build());

        profile.setResumeText(resumeText);
        profile.setExtractedSkills(extractedSkills);

        userProfileRepository.save(profile);
        log.debug("Updated user profile with resume data for user: {}", userId);
    }
}
