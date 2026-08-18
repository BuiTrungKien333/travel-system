package com.travel.common.exception;

import com.travel.common.response.ErrorResponse;
import com.travel.common.util.Translator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Translator translator;

    /*
    * MethodArgumentNotValidException
    * Xử lý vi phạm ràng buộc dữ liệu khi gửi request lên server (DTO)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message(translator.toLocale(ErrorCode.VALIDATION_ERROR.getMessageKey()))
                .errors(errors)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(response);

    }

    /*
   ConstraintViolationException
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message(translator.toLocale(ErrorCode.VALIDATION_ERROR.getMessageKey()))
                .errors(errors)
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(response);
    }


    /*
     * Security / Authorization Exception
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("Access denied on uri: {} - {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(ErrorCode.FORBIDDEN);
    }

    /*
     * Business Exception
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("AppException: Code {} - MessageKey {}", errorCode.getCode(), errorCode.getMessageKey());

        return buildErrorResponse(errorCode);
    }

    /*
     * Fallback Exception
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Uncategorized Error", e);
        return buildErrorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(ErrorCode errorCode) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(translator.toLocale(errorCode.getMessageKey()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }
}

