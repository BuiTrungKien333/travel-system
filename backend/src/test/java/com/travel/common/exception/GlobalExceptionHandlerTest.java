package com.travel.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.travel.common.response.ErrorResponse;
import com.travel.common.util.Translator;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private Translator translator;

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler(translator);
        when(request.getRequestURI()).thenReturn("/api/v1/public/weather/current");
        when(translator.toLocale(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should properly format MethodArgumentTypeMismatchException (e.g. longitude='a')")
    void handleMethodArgumentTypeMismatchException_returns400WithDetails() {
        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "a", double.class, "longitude", methodParameter, new NumberFormatException("For input string: \"a\"")
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatchException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("COMMON_008");
        assertThat(response.getBody().getErrors()).containsKey("longitude");
        assertThat(response.getBody().getErrors().get("longitude"))
                .contains("Tham số 'longitude' nhận giá trị không hợp lệ: 'a'")
                .contains("Kiểu dữ liệu yêu cầu là 'double'");
    }

    @Test
    @DisplayName("Should properly format MissingServletRequestParameterException (e.g. missing latitude)")
    void handleMissingServletRequestParameterException_returns400WithDetails() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("latitude", "double");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMissingServletRequestParameterException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo("COMMON_009");
        assertThat(response.getBody().getErrors()).containsKey("latitude");
        assertThat(response.getBody().getErrors().get("latitude"))
                .contains("Yêu cầu thiếu tham số bắt buộc 'latitude'");
    }

    @Test
    @DisplayName("Should extract field details for HttpMessageNotReadableException with InvalidFormatException")
    void handleHttpMessageNotReadableException_withInvalidFormat_returnsFieldDetails() {
        InvalidFormatException cause = new InvalidFormatException(
                null, "Cannot deserialize value", "not-a-number", Double.class
        );
        cause.prependPath(new JsonMappingException.Reference(null, "latitude"));

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid JSON", cause, null);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadableException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COMMON_006");
        assertThat(response.getBody().getErrors()).containsKey("latitude");
    }

    @Test
    @DisplayName("Should properly handle NoResourceFoundException (404)")
    void handleNoResourceFoundException_returns404() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COMMON_004");
        assertThat(response.getBody().getErrors()).containsKey("path");
    }

    @Test
    @DisplayName("Should properly handle IllegalArgumentException")
    void handleIllegalArgumentException_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Vĩ độ không thể vượt quá 90 độ");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("COMMON_011");
        assertThat(response.getBody().getErrors()).containsEntry("error", "Vĩ độ không thể vượt quá 90 độ");
    }
}
