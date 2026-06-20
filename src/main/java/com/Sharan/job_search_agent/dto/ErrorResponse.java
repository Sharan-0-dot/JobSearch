package com.Sharan.job_search_agent.dto;

import java.time.LocalDateTime;

public record ErrorResponse(LocalDateTime timestamp, String error, String message) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(LocalDateTime.now(), error, message);
    }
}