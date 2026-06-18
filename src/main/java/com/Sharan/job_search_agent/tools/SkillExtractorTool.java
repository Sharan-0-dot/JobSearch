package com.Sharan.job_search_agent.tools;

import com.Sharan.job_search_agent.service.SkillExtractionService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillExtractorTool {

    private final SkillExtractionService skillExtractionService;

    @Tool("""
    Extract technical skills, frameworks, tools,
    programming languages, databases, and technologies
    from provided text.
    
    Use when:
    - user provides resume text
    - user provides job description
    - skills need to be identified automatically
    
    Returns only extracted skills.
    """)
    public String extractSkills(@P("The text to extract skills from (resume, job description, etc.)") String text) {
        log.info("SkillExtractorTool invoked | text length: {}", text.length());

        String[] skills = skillExtractionService.extractSkillsFromText(text);

        if (skills.length == 0) {
            return "No skills could be extracted from the provided text.";
        }

        return "Extracted " + skills.length + " skills: " + String.join(", ", skills);
    }
}