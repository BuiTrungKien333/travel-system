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
public class DatabaseHealthController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthController(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Operation(summary = "Check PostgreSQL database connectivity", description = "Executes a ping query to verify database connection status")
    @GetMapping("/api/v1/health/database")
    public ApiResponse<Map<String, String>> databaseHealth() {

        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        return ApiResponse.success(
                Map.of(
                        "status", "UP",
                        "database",
                        result != null && result == 1
                                ? "CONNECTED"
                                : "UNKNOWN"
                )
        );
    }
}
