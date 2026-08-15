package com.travel.health;

import com.travel.common.response.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DatabaseHealthController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthController(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
