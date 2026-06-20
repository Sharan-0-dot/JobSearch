package com.Sharan.job_search_agent.controller;

import com.Sharan.job_search_agent.agent.JobSearchAgent;
import com.Sharan.job_search_agent.agent.PostgresChatMemoryStore;
import com.Sharan.job_search_agent.dto.AgentQueryRequest;
import com.Sharan.job_search_agent.dto.AgentQueryResponse;
import com.Sharan.job_search_agent.dto.ErrorResponse;
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
    public ResponseEntity<?> query(@RequestBody AgentQueryRequest request) {

        String userId = request.userId();
        String userMessage = request.query();

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(errorBody("Bad Request", "userId is required"));
        }

        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(errorBody("Bad Request", "query is required"));
        }

        InputValidationService.ValidationResult validation =
                inputValidationService.validate(userMessage);

        if (!validation.isValid()) {
            log.warn("Rejected query from user {}. Reason: {}", userId, validation.rejectionReason());

            return ResponseEntity.badRequest()
                    .body(errorBody(
                            "Invalid input",
                            validation.rejectionReason()
                    ));
        }

        userMessage = validation.sanitizedInput();

        log.info("Agent query | userId={} | query={}", userId, userMessage);

        agentQueryCounter.increment();

        long startTime = System.currentTimeMillis();

        try {

            Result<String> result =
                    jobSearchAgent.chat(userId, userMessage);

            String response = result.content();

            if (result.tokenUsage() != null) {
                tokenUsageTracker.record(result.tokenUsage());
            }

            List<String> toolsUsed = result.toolExecutions()
                    .stream()
                    .map(toolExecution -> toolExecution.request().name())
                    .distinct()
                    .toList();

            boolean usedJobTool =
                    toolsUsed.contains("searchLiveJobs")
                            || toolsUsed.contains("findMatchingJobs");

            if (looksLikeJobSearchQuery(userMessage) && !usedJobTool) {

                log.warn(
                        "Possible hallucination detected. No job tool executed. userId={} query={}",
                        userId,
                        userMessage
                );

                return ResponseEntity.ok(AgentQueryResponse.guardrailTriggered(userId, userMessage, toolsUsed));
            }

            long executionTime =
                    System.currentTimeMillis() - startTime;

            log.info(
                    "Agent responded in {} ms | userId={} | tools={}",
                    executionTime,
                    userId,
                    toolsUsed
            );

            executionTraceService.saveTrace(
                    userId,
                    userMessage,
                    response,
                    toolsUsed,
                    executionTime
            );

            return ResponseEntity.ok(
                    AgentQueryResponse.success(
                            userId,
                            userMessage,
                            response,
                            toolsUsed,
                            executionTime
                    )
            );

        } catch (Exception e) {

            long executionTime =
                    System.currentTimeMillis() - startTime;

            log.error(
                    "Agent error for user {}",
                    userId,
                    e
            );

            return ResponseEntity.internalServerError()
                    .body(errorBody(
                            "Agent processing failed",
                            e.getMessage()
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
                executionTraceService.getTracesByUser(userId, pageable)
                        .map(executionTraceService::toDto)
        );
    }

    private boolean looksLikeJobSearchQuery(String query) {

        String q = query.toLowerCase();

        return q.contains("job")
                || q.contains("jobs")
                || q.contains("intern")
                || q.contains("internship")
                || q.contains("opening")
                || q.contains("openings")
                || q.contains("vacancy")
                || q.contains("vacancies")
                || q.contains("hiring")
                || q.contains("position")
                || q.contains("positions")
                || q.contains("developer")
                || q.contains("software engineer")
                || q.contains("backend")
                || q.contains("frontend")
                || q.contains("full stack")
                || q.contains("role")
                || q.contains("roles");
    }

    private ErrorResponse errorBody(String error, String message) {
        return ErrorResponse.of(error, message);
    }
}