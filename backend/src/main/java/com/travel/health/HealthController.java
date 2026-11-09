package com.travel.health;

import com.travel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health Check", description = "Endpoints for monitoring system health and connectivity")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Operation(summary = "Check system and database health", description = "Returns operational status of the service and database connectivity")
    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, String>> health() {
        String dbStatus;
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbStatus = (result != null && result == 1) ? "CONNECTED" : "UNKNOWN";
        } catch (Exception ex) {
            dbStatus = "DISCONNECTED";
        }

        return ApiResponse.success(
                Map.of(
                        "status", "UP",
                        "database", dbStatus
                )
        );
    }
}
