package com.travel.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION("COMMON_001", "error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR("COMMON_002", "error.internal.server", HttpStatus.INTERNAL_SERVER_ERROR),

    VALIDATION_ERROR("COMMON_003", "error.validation.fail", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("COMMON_004", "error.validation.resource-not-found", HttpStatus.NOT_FOUND),
    UNSUPPORTED_MEDIA_TYPE("COMMON_005", "error.unsupported.media-type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    MALFORMED_REQUEST_BODY("COMMON_006", "error.malformed.request-body", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("COMMON_007", "error.method.not-allowed", HttpStatus.METHOD_NOT_ALLOWED),
    INVALID_PARAMETER_TYPE("COMMON_008", "error.param.type-mismatch", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_PARAMETER("COMMON_009", "error.param.missing", HttpStatus.BAD_REQUEST),
    MISSING_PATH_VARIABLE("COMMON_010", "error.path.missing", HttpStatus.BAD_REQUEST),
    INVALID_ARGUMENT("COMMON_011", "error.argument.invalid", HttpStatus.BAD_REQUEST),

    // --- AUTH & PERMISSION CODES ---
    UNAUTHORIZED("AUTH_001", "error.auth.unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH_002", "error.auth.forbidden", HttpStatus.FORBIDDEN),
    USER_ALREADY_EXISTS("AUTH_003", "error.auth.email-exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("AUTH_004", "error.auth.invalid-credentials", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("AUTH_005", "error.auth.invalid-refresh-token", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("AUTH_006", "error.auth.user-not-found", HttpStatus.NOT_FOUND),
    ACCOUNT_LOCKED_OR_DELETED("AUTH_007", "error.auth.account-disabled", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED("AUTH_008", "error.auth.email-not-verified", HttpStatus.FORBIDDEN),
    INVALID_ACTION_TOKEN("AUTH_009", "error.auth.invalid-action-token", HttpStatus.BAD_REQUEST),
    EMAIL_SERVICE_UNAVAILABLE("AUTH_010", "error.auth.email-unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_GOOGLE_TOKEN("AUTH_011", "error.auth.google-invalid", HttpStatus.UNAUTHORIZED),
    GOOGLE_SERVICE_UNAVAILABLE("AUTH_012", "error.auth.google-unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    GOOGLE_NOT_CONFIGURED("AUTH_013", "error.auth.google-not-configured", HttpStatus.SERVICE_UNAVAILABLE),
    GOOGLE_ACCOUNT_CONFLICT("AUTH_014", "error.auth.google-conflict", HttpStatus.CONFLICT),

    TRIP_NOT_FOUND("TRIP_001", "error.trip.not-found", HttpStatus.NOT_FOUND),
    TRIP_VERSION_CONFLICT("TRIP_002", "error.trip.version-conflict", HttpStatus.CONFLICT),
    TRIP_DATE_IMPACT("TRIP_003", "error.trip.date-impact", HttpStatus.CONFLICT),
    TRIP_REQUEST_CONFLICT("TRIP_004", "error.trip.request-conflict", HttpStatus.CONFLICT),
    TRIP_INVALID_REFERENCE("TRIP_005", "error.trip.invalid-reference", HttpStatus.BAD_REQUEST),
    TRIP_READ_ONLY("TRIP_006", "error.trip.read-only", HttpStatus.CONFLICT),

    // --- PLANNING & ALGORITHM CODES ---
    TSP_EXCEEDS_MAX_LOCATIONS("PLANNING_001", "error.planning.tsp-exceeds-max-locations", HttpStatus.BAD_REQUEST),
    INVALID_TSP_LOCATIONS("PLANNING_002", "error.planning.invalid-tsp-locations", HttpStatus.BAD_REQUEST),

    // --- AI PROVIDER CODES ---
    AI_SERVICE_UNAVAILABLE("AI_001", "error.ai.service-unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    AI_RESPONSE_PARSE_FAILED("AI_002", "error.ai.parse-failed", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_RATE_LIMIT_EXCEEDED("AI_003", "error.ai.rate-limit-exceeded", HttpStatus.TOO_MANY_REQUESTS),
    AI_API_KEY_INVALID("AI_004", "error.ai.api-key-invalid", HttpStatus.UNAUTHORIZED),
    AI_VALIDATION_FAILED("AI_005", "error.ai.validation-failed", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String code;
    private final String messageKey;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String messageKey, HttpStatus httpStatus) {
        this.code = code;
        this.messageKey = messageKey;
        this.httpStatus = httpStatus;
    }
}
