package com.travel.common.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.travel.common.response.ErrorResponse;
import com.travel.common.util.Translator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Translator translator;

    /*
     * 1. MethodArgumentNotValidException
     * Xử lý vi phạm ràng buộc dữ liệu Bean Validation trên RequestBody (@Valid DTO)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        log.warn("Validation error on uri: {} - fields: {}", request.getRequestURI(), errors.keySet());
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, errors);
    }

    /*
     * 2. ConstraintViolationException
     * Xử lý vi phạm ràng buộc @Validated cấp độ class hoặc JPA validation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        log.warn("Constraint violation on uri: {} - details: {}", request.getRequestURI(), errors);
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, errors);
    }

    /*
     * 3. HandlerMethodValidationException
     * Xử lý lỗi validate trực tiếp trên @RequestParam/@PathVariable trong controller (Spring Boot 3/4)
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        exception.getParameterValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error ->
                    errors.put(paramName != null ? paramName : "parameter", error.getDefaultMessage())
            );
        });

        log.warn("Method parameter validation failed on uri: {} - {}", request.getRequestURI(), errors);
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, errors);
    }

    /*
     * 4. MethodArgumentTypeMismatchException
     * Xử lý trường hợp gửi sai kiểu dữ liệu query param hoặc path variable (ví dụ: longitude=a thay vì số thực, UUID sai format...)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String paramName = e.getName();
        Object invalidValue = e.getValue();
        Class<?> requiredType = e.getRequiredType();
        String requiredTypeName = requiredType != null ? requiredType.getSimpleName() : "không xác định";

        String detailMessage = String.format(
                "Tham số '%s' nhận giá trị không hợp lệ: '%s'. Kiểu dữ liệu yêu cầu là '%s'",
                paramName, invalidValue, requiredTypeName
        );

        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(paramName, detailMessage);

        log.warn("Parameter type mismatch on uri: {} - param '{}', value '{}', expected type '{}'",
                request.getRequestURI(), paramName, invalidValue, requiredTypeName);

        return buildErrorResponse(ErrorCode.INVALID_PARAMETER_TYPE, errors);
    }

    /*
     * 5. MissingServletRequestParameterException
     * Xử lý trường hợp thiếu query param bắt buộc (@RequestParam)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        String paramName = e.getParameterName();
        String paramType = e.getParameterType();

        String detailMessage = String.format(
                "Yêu cầu thiếu tham số bắt buộc '%s' (kiểu: %s)",
                paramName, paramType
        );

        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(paramName, detailMessage);

        log.warn("Missing request parameter on uri: {} - param '{}' ({})",
                request.getRequestURI(), paramName, paramType);

        return buildErrorResponse(ErrorCode.MISSING_REQUIRED_PARAMETER, errors);
    }

    /*
     * 6. MissingPathVariableException
     * Xử lý trường hợp thiếu biến đường dẫn (@PathVariable)
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVariableException(
            MissingPathVariableException e, HttpServletRequest request) {
        String varName = e.getVariableName();
        String detailMessage = String.format("Thiếu biến đường dẫn bắt buộc '%s'", varName);

        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(varName, detailMessage);

        log.warn("Missing path variable on uri: {} - variable '{}'", request.getRequestURI(), varName);

        return buildErrorResponse(ErrorCode.MISSING_PATH_VARIABLE, errors);
    }

    /*
     * 7. HttpMessageNotReadableException
     * Xử lý trường hợp body JSON sai cú pháp hoặc kiểu dữ liệu các trường trong JSON không khớp
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("Malformed request body on uri: {} - {}", request.getRequestURI(), e.getMessage());

        Map<String, String> errors = new LinkedHashMap<>();
        Throwable cause = e.getCause();

        if (cause instanceof InvalidFormatException ife) {
            String fieldPath = ife.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            String targetType = ife.getTargetType() != null ? ife.getTargetType().getSimpleName() : "hợp lệ";
            String invalidValue = String.valueOf(ife.getValue());
            errors.put(fieldPath.isEmpty() ? "body" : fieldPath,
                    String.format("Giá trị '%s' không thể chuyển đổi sang kiểu %s", invalidValue, targetType));
        } else if (cause instanceof JsonParseException jpe) {
            errors.put("json", String.format(
                    "Cú pháp JSON không hợp lệ tại dòng %d, cột %d: %s",
                    jpe.getLocation().getLineNr(), jpe.getLocation().getColumnNr(), jpe.getOriginalMessage()
            ));
        }

        return buildErrorResponse(ErrorCode.MALFORMED_REQUEST_BODY, errors.isEmpty() ? null : errors);
    }

    /*
     * 8. HttpMediaTypeNotSupportedException
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("Unsupported Media Type on uri: {} - Content-Type '{}' not supported",
                request.getRequestURI(), e.getContentType());
        return buildErrorResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null);
    }

    /*
     * 9. HttpRequestMethodNotSupportedException
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not allowed on uri: {} - Method '{}' not supported",
                request.getRequestURI(), e.getMethod());
        return buildErrorResponse(ErrorCode.METHOD_NOT_ALLOWED, null);
    }

    /*
     * 10. NoResourceFoundException / NoHandlerFoundException (404 Not Found)
     * Bắt lỗi khi người dùng gọi sai đường dẫn endpoint
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(Exception e, HttpServletRequest request) {
        log.warn("Endpoint not found: uri={}", request.getRequestURI());
        Map<String, String> errors = Map.of(
                "path", String.format("Đường dẫn '%s' không tồn tại trên hệ thống", request.getRequestURI())
        );
        return buildErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, errors);
    }

    /*
     * MaxUploadSizeExceededException
     * Spring throw khi file hoặc tổng request vượt quá giới hạn multipart config.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("Upload size exceeded on uri: {} - {}", request.getRequestURI(), e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .code("UPLOAD_002")
                .message(translator.toLocale("error.upload.max-size-exceeded"))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /*
     * 11. IllegalArgumentException / IllegalStateException
     * Xử lý các tham số logic nghiệp vụ không hợp lệ
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            RuntimeException e, HttpServletRequest request) {
        log.warn("Invalid argument/state on uri: {} - {}", request.getRequestURI(), e.getMessage());
        Map<String, String> errors = Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "Yêu cầu không hợp lệ"
        );
        return buildErrorResponse(ErrorCode.INVALID_ARGUMENT, errors);
    }

    /*
     * 12. Security / Authentication & Authorization Exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        log.warn("Authentication required on uri: {} - {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(ErrorCode.UNAUTHORIZED, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        log.warn("Access denied on uri: {} - {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(ErrorCode.FORBIDDEN, null);
    }

    /*
     * 13. Business Exception (AppException)
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("Business exception [{}]: uri={}, messageKey={}",
                errorCode.getCode(), request.getRequestURI(), errorCode.getMessageKey());
        return buildErrorResponse(errorCode, null);
    }

    @ExceptionHandler(org.springframework.dao.InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccessApiUsageException(Exception e, HttpServletRequest request) {
        log.warn("Invalid API parameter/data usage on uri: {} - {}", request.getRequestURI(), e.getMessage());
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR, null);
    }

    /*
     * 14. Fallback Exception (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled internal exception on uri: {}", request.getRequestURI(), e);
        return buildErrorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION, null);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(ErrorCode errorCode, Map<String, String> errors) {
        ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(translator.toLocale(errorCode.getMessageKey()))
                .timestamp(Instant.now());

        if (errors != null && !errors.isEmpty()) {
            builder.errors(errors);
        }

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(builder.build());
    }
}
