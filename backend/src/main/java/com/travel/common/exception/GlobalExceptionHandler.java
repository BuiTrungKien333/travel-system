package com.travel.common.exception;

import com.travel.common.response.ErrorResponse;
import com.travel.common.util.Translator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
     * ConstraintViolationException
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
     * HTTP / Web Protocol Exceptions (Media Type, Malformed Body, Method Not Allowed)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("Unsupported Media Type on uri: {} - Content-Type '{}' not supported", request.getRequestURI(), e.getContentType());
        return buildErrorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("Malformed request body on uri: {} - {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(ErrorCode.MALFORMED_REQUEST_BODY);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not allowed on uri: {} - Method '{}' not supported", request.getRequestURI(), e.getMethod());
        return buildErrorResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /*
     * Security / Authentication & Authorization Exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("Authentication required on uri: {} - {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(ErrorCode.UNAUTHORIZED);
    }

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
        log.warn("Business exception [{}]: uri={}, messageKey={}", errorCode.getCode(), request.getRequestURI(), errorCode.getMessageKey());

        return buildErrorResponse(errorCode);
    }

    @ExceptionHandler({org.springframework.dao.InvalidDataAccessApiUsageException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleInvalidDataAccessApiUsageException(Exception e, HttpServletRequest request) {
        log.warn("Invalid API parameter/data usage on uri: {} - {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR);
    }

    /*
     * Fallback Exception (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled internal exception on uri: {}", request.getRequestURI(), e);
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
