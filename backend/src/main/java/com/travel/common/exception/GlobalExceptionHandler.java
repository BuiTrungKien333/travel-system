package com.travel.common.exception;

import com.travel.common.response.ErrorResponse;
import com.travel.common.util.Translator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Translator translator;

    /*
     * Business Exception
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("AppException: Code {} - MessageKey {}", errorCode.getCode(), errorCode.getMessageKey());

        return buildErrorResponse(errorCode, request.getRequestURI());
    }

    /*
     * Fallback Exception
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Uncategorized Error", e);
        return buildErrorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION, request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(ErrorCode errorCode, String path) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(translator.toLocale(errorCode.getMessageKey()))
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }
}

