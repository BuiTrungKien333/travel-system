package com.travel.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard system error response payload")
public class ErrorResponse {

    @Schema(description = "Indicates request failure (always false on error)", example = "false")
    private final boolean success;

    @Schema(description = "System error code identifier", example = "AUTH_001")
    private final String code;

    @Schema(description = "Localized error message explaining the failure", example = "Unauthorized access")
    private final String message;

    @Schema(description = "Detailed field validation errors mapping (if applicable)")
    private final Map<String, String> errors;

    @Schema(description = "Timestamp when the error occurred", example = "2026-08-20T12:00:00Z")
    private final Instant timestamp;
}
