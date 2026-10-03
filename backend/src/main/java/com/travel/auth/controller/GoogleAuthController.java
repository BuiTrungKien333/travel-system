package com.travel.auth.controller;

import com.travel.auth.dto.request.*;
import com.travel.auth.dto.response.*;
import com.travel.auth.service.GoogleAuthService;
import com.travel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/google")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService service;

    // TODO: Apply the same IP/account rate limits as other auth endpoints before public release.
    @PostMapping("/challenge")
    @Operation(summary = "Start Google sign-in", description = "Short-lived single-use challenge; pass nonce into Google SDK. Web client ID is public configuration.")
    public ResponseEntity<ApiResponse<GoogleChallengeResponse>> challenge() {
        return ResponseEntity.ok(ApiResponse.success("Google sign-in challenge created", service.challenge()));
    }

    @PostMapping
    @Operation(summary = "Sign in with Google", description = "Server verifies Google JWT and nonce. Gmail/Workspace auto-link by email; third-party email requires confirmation. Does not accept client profile/role.")
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> login(@Valid @RequestBody GoogleLoginRequest request,
                                                                  @RequestHeader(value = "Accept-Language", defaultValue = "vi") String locale) {
        var result = service.login(request, locale);
        return ResponseEntity.status(result.status() == GoogleLoginResponse.Status.AUTHENTICATED ? 200 : 202)
                .body(ApiResponse.success("Google sign-in processed", result));
    }

    @PostMapping("/verify-link")
    @Operation(summary = "Confirm third-party email and sign in", description = "Explicit one-use email confirmation; creates a normal application session. Opening the link never consumes it.")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyLink(@Valid @RequestBody ActionTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Google sign-in successful", service.verifyLink(request.token())));
    }
}
