package com.Sharan.job_search_agent.tools;

import com.Sharan.job_search_agent.service.SkillExtractionService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillExtractorTool {

    private final SkillExtractionService skillExtractionService;

    @Tool("Extract technical skills, tools, and technologies from any text. " +
            "Use this to parse skills from a job description or resume snippet.")
    public String extractSkills(String text) {
        log.info("SkillExtractorTool invoked | text length: {}", text.length());

        String[] skills = skillExtractionService.extractSkillsFromText(text);

        if (skills.length == 0) {
            return "No skills could be extracted from the provided text.";
        }

        return "Extracted " + skills.length + " skills: " + String.join(", ", skills);
    }
}