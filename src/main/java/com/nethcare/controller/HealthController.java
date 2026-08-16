package com.nethcare.controller;

import com.nethcare.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check controller — confirms the application is running.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "application", "Nethcare Management System",
                "version", "0.1.0-SNAPSHOT",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
