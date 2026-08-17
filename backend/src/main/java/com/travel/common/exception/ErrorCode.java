package com.travel.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION("COMMON_001", "error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR("COMMON_002", "error.internal.server", HttpStatus.INTERNAL_SERVER_ERROR),

    VALIDATION_ERROR("COMMON_003", "error.validation.fail", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("COMMON_004", "error.validation.resource-not-found", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String messageKey;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String messageKey, HttpStatus httpStatus) {
        this.code = code;
        this.messageKey = messageKey;
        this.httpStatus = httpStatus;
    }
}
