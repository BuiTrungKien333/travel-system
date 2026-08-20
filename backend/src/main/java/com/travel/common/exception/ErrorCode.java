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

    // --- AUTH & PERMISSION CODES ---
    UNAUTHORIZED("AUTH_001", "error.auth.unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH_002", "error.auth.forbidden", HttpStatus.FORBIDDEN),
    USER_ALREADY_EXISTS("AUTH_003", "error.auth.email-exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("AUTH_004", "error.auth.invalid-credentials", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("AUTH_005", "error.auth.invalid-refresh-token", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("AUTH_006", "error.auth.user-not-found", HttpStatus.NOT_FOUND),
    ACCOUNT_LOCKED_OR_DELETED("AUTH_007", "error.auth.account-disabled", HttpStatus.FORBIDDEN);

    private final String code;
    private final String messageKey;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String messageKey, HttpStatus httpStatus) {
        this.code = code;
        this.messageKey = messageKey;
        this.httpStatus = httpStatus;
    }
}
