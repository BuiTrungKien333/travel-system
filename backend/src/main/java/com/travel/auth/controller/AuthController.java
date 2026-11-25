package com.travel.auth.controller;

import com.travel.auth.dto.request.LoginRequest;
import com.travel.auth.dto.request.RefreshTokenRequest;
import com.travel.auth.dto.request.RegisterRequest;
import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.dto.response.RegistrationResponse;
import com.travel.auth.service.AuthService;
import com.travel.common.response.ApiResponse;
import com.travel.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and authorization")
@Slf4j(topic = "AUTH_CONTROLLER")
public class AuthController {

    private final AuthService authService;

    // TODO: Nên thêm rate limit cho login/register/refresh trước khi public để hạn chế brute force và spam.
    // Có thể thực hiện tại gateway; không cần thêm Redis vào base MVP chỉ cho mục đích này.

    // Register
    @Operation(
        summary = "Register new user",
        description = "Create an unverified USER account and request a single-use email verification link; no credentials are issued"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "202",
            description = "Registration accepted; email verification required"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation failure on input fields",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = "{\"success\": false, \"code\": \"COMMON_003\", \"message\": \"Validation failed\", \"errors\": {\"email\": \"Invalid email format\", \"password\": \"Password must be between 8 and 50 characters\"}, \"timestamp\": \"2026-08-20T12:00:00Z\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Email is already registered in the system",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Conflict Error",
                    value = "{\"success\": false, \"code\": \"AUTH_003\", \"message\": \"Email already exists\", \"timestamp\": \"2026-08-20T12:00:00Z\"}"
                )
            )
        ),
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(
        @Valid @RequestBody RegisterRequest request,
        @RequestHeader(value = "Accept-Language", defaultValue = "vi") String locale
    ) {
        RegistrationResponse response = authService.register(request, locale);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            ApiResponse.success("Registration accepted; verify your email before signing in", response)
        );
    }

    // Login
    @Operation(
        summary = "User login",
        description = "Authenticate user with email and password to receive JWT access and refresh tokens"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful, tokens generated"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation failure on input fields",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = "{\"success\": false, \"code\": \"COMMON_003\", \"message\": \"Validation failed\", \"errors\": {\"email\": \"Email is required\", \"password\": \"Password is required\"}, \"timestamp\": \"2026-08-20T12:00:00Z\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid email or password",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Unauthorized Error",
                    value = "{\"success\": false, \"code\": \"AUTH_004\", \"message\": \"Invalid email or password\", \"timestamp\": \"2026-08-20T12:00:00Z\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Account is locked, deactivated or email is not verified (AUTH_008)",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Forbidden Error",
                    value = "{\"success\": false, \"code\": \"AUTH_007\", \"message\": \"Account has been locked or deactivated\", \"timestamp\": \"2026-08-20T12:00:00Z\"}"
                )
            )
        ),
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // Refresh Token
    @Operation(
        summary = "Refresh access token",
        description = "Provide a valid refresh token to obtain a new pair of access and refresh tokens"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully, new tokens generated"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation failure on input fields",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = "{\"success\": false, \"code\": \"COMMON_003\", \"message\": \"Validation failed\", \"errors\": {\"refreshToken\": \"Refresh token is required\"}, \"timestamp\": \"2026-08-20T12:00:00Z\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Unauthorized Error",
                    value = "{\"success\": false, \"code\": \"AUTH_005\", \"message\": \"Invalid or expired refresh token\", \"timestamp\": \"2026-08-20T12:00:00Z\"}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Account is locked or deactivated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Forbidden Error",
                    value = "{\"success\": false, \"code\": \"AUTH_007\", \"message\": \"Account has been locked or deactivated\", \"timestamp\": \"2026-08-20T12:00:00Z\"}"
                )
            )
        ),
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Log out current device",
        description = "Idempotently revoke the supplied refresh token. Existing access tokens expire after their configured lifetime."
    )
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
