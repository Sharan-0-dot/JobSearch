package com.Sharan.job_search_agent.controller;

import com.Sharan.job_search_agent.agent.JobSearchAgent;
import com.Sharan.job_search_agent.agent.PostgresChatMemoryStore;
import com.Sharan.job_search_agent.observability.TokenUsageTracker;
import com.Sharan.job_search_agent.service.ExecutionTraceService;
import com.Sharan.job_search_agent.validation.InputValidationService;
import dev.langchain4j.service.Result;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final JobSearchAgent jobSearchAgent;
    private final ExecutionTraceService executionTraceService;
    private final PostgresChatMemoryStore chatMemoryStore;
    private final TokenUsageTracker tokenUsageTracker;
    private final Counter agentQueryCounter;
    private final InputValidationService inputValidationService;

    public AgentController(
            JobSearchAgent jobSearchAgent,
            ExecutionTraceService executionTraceService,
            PostgresChatMemoryStore chatMemoryStore,
            TokenUsageTracker tokenUsageTracker,
            Counter agentQueryCounter,
            InputValidationService inputValidationService) {
        this.jobSearchAgent = jobSearchAgent;
        this.executionTraceService = executionTraceService;
        this.chatMemoryStore = chatMemoryStore;
        this.tokenUsageTracker = tokenUsageTracker;
        this.agentQueryCounter = agentQueryCounter;
        this.inputValidationService = inputValidationService;
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String userMessage = request.get("query");

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId is required"));
        }
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "query is required"));
        }

        InputValidationService.ValidationResult validation =
                inputValidationService.validate(userMessage);

        if (!validation.isValid()) {
            log.warn("Rejected query from user {} — reason: {}",
                    userId, validation.rejectionReason());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Invalid input",
                            "reason", validation.rejectionReason()
                    ));
        }

        userMessage = validation.sanitizedInput();

        log.info("Agent query | userId: {} | query: {}", userId, userMessage);

        agentQueryCounter.increment();

        long startTime = System.currentTimeMillis();

        try {

            Result<String> result = jobSearchAgent.chat(userId, userMessage);

            String response = result.content();

            tokenUsageTracker.record(result.tokenUsage());

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Agent responded in {}ms for user: {}", executionTime, userId);

            List<String> toolsUsed = detectToolsUsed(response);

            executionTraceService.saveTrace(
                    userId, userMessage, response, toolsUsed, executionTime);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "query", userMessage,
                    "response", response,
                    "toolsUsed", toolsUsed,
                    "executionTimeMs", executionTime
            ));

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Agent error for user {}: {}", userId, e.getMessage());

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Agent processing failed",
                            "message", e.getMessage(),
                            "executionTimeMs", executionTime
                    ));
        }
    }

    @DeleteMapping("/memory/{userId}")
    public ResponseEntity<?> clearMemory(@PathVariable String userId) {
        chatMemoryStore.deleteMessages(userId);
        return ResponseEntity.ok(Map.of(
                "message", "Memory cleared for user: " + userId
        ));
    }

    @GetMapping("/trace/{userId}")
    public ResponseEntity<?> getTraces(
            @PathVariable String userId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                executionTraceService.getTracesByUser(userId, pageable));
    }

    private List<String> detectToolsUsed(String response) {
        List<String> tools = new ArrayList<>();
        if (response == null) return tools;

        String lower = response.toLowerCase();
        if (lower.contains("live") || lower.contains("jsearch") || lower.contains("fetched"))
            tools.add("JSearchTool");
        if (lower.contains("match score") || lower.contains("semantic") || lower.contains("cached"))
            tools.add("JobRagTool");
        if (lower.contains("profile") || lower.contains("your skills"))
            tools.add("UserProfileTool");
        if (lower.contains("extracted") && lower.contains("skill"))
            tools.add("SkillExtractorTool");

        return tools;
    }
}