package com.Sharan.job_search_agent.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillExtractionService {

    private final ChatLanguageModel chatLanguageModel;

    private static final List<String> KNOWN_SKILLS = List.of(
            // Languages
            "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust", "C\\+\\+", "C#", "Kotlin", "Swift",
            // Backend
            "Spring Boot", "Spring", "Django", "FastAPI", "Flask", "Node.js", "Express",
            "Micronaut", "Quarkus", "Hibernate", "JPA",
            // Frontend
            "React", "Angular", "Vue", "Next.js", "HTML", "CSS", "Tailwind",
            // Databases
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Cassandra", "DynamoDB",
            "Elasticsearch", "pgvector", "Oracle",
            // Cloud & DevOps
            "AWS", "GCP", "Azure", "Docker", "Kubernetes", "Terraform", "Ansible",
            "CI/CD", "Jenkins", "GitHub Actions", "ArgoCD",
            // Messaging
            "Kafka", "RabbitMQ", "ActiveMQ", "SQS",
            // AI/ML
            "LangChain", "LangChain4j", "Ollama", "TensorFlow", "PyTorch", "Scikit-learn",
            "OpenAI", "Gemini", "Hugging Face", "RAG", "pgvector",
            // Tools
            "Git", "Maven", "Gradle", "Linux", "Bash", "REST", "GraphQL", "gRPC",
            "Microservices", "Distributed Systems", "System Design",
            // Testing
            "JUnit", "Mockito", "Pytest", "Selenium", "Postman"
    );

    // Pre-compile patterns for performance — case insensitive
    private static final List<Pattern> SKILL_PATTERNS = KNOWN_SKILLS.stream()
            .map(skill -> Pattern.compile("\\b" + skill + "\\b", Pattern.CASE_INSENSITIVE))
            .collect(Collectors.toList());


    public String[] extractSkillsFromText(String text) {
        if (text == null || text.isBlank()) return new String[0];

        // Step 1 — Regex pass (fast, deterministic)
        Set<String> regexSkills = extractWithRegex(text);
        log.debug("Regex extracted {} skills", regexSkills.size());

        // Step 2 — LLM pass (catches aliases, abbreviations, unlisted skills)
        Set<String> llmSkills = extractWithLlm(text);
        log.debug("LLM extracted {} skills", llmSkills.size());

        // Merge both sets, deduplicate
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(regexSkills);
        merged.addAll(llmSkills);

        log.info("Total extracted skills: {}", merged.size());
        return merged.toArray(new String[0]);
    }


    private Set<String> extractWithRegex(String text) {
        Set<String> found = new LinkedHashSet<>();
        for (int i = 0; i < SKILL_PATTERNS.size(); i++) {
            if (SKILL_PATTERNS.get(i).matcher(text).find()) {
                found.add(KNOWN_SKILLS.get(i).replace("\\+\\+", "++"));
            }
        }
        return found;
    }


    private Set<String> extractWithLlm(String text) {
        try {

            String truncated = text.length() > 3000 ? text.substring(0, 3000) : text;

            String prompt = """
                    Extract all technical skills, tools, frameworks, programming languages, 
                    and technologies mentioned in the following text.
                    
                    Rules:
                    - Return ONLY a comma-separated list of skill names
                    - No explanations, no numbering, no extra text
                    - Normalize names: "JS" → "JavaScript", "PG" → "PostgreSQL"
                    - Skip soft skills like "communication", "teamwork"
                    
                    Text:
                    """ + truncated;

            String response = chatLanguageModel.generate(prompt);

            return parseSkillsFromCsv(response);

        } catch (Exception e) {
            log.warn("LLM skill extraction failed, using regex results only: {}", e.getMessage());
            return Collections.emptySet();
        }
    }


    private Set<String> parseSkillsFromCsv(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptySet();

        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.length() > 1)         // skip single-char noise
                .filter(s -> s.length() < 50)        // skip sentences that slipped through
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}