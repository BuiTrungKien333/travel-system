package com.travel.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final java.util.Map<String, String> fieldErrors;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.fieldErrors = null;
    }

    public AppException(ErrorCode errorCode, java.util.Map<String, String> fieldErrors) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.fieldErrors = java.util.Map.copyOf(fieldErrors);
    }
}
