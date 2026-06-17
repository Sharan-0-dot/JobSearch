package com.Sharan.job_search_agent.controller;

import com.Sharan.job_search_agent.service.ExecutionTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/trace")
@RequiredArgsConstructor
public class TraceController {

    private final ExecutionTraceService executionTraceService;

    @GetMapping("/{traceId}")
    public ResponseEntity<?> getTrace(@PathVariable UUID traceId) {

        return executionTraceService.getTrace(traceId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}