package com.travel.auth.controller;

import com.travel.auth.dto.request.*;
import com.travel.auth.dto.response.EmailRequestResponse;
import com.travel.auth.entity.ActionPurpose;
import com.travel.auth.service.EmailActionService;
import com.travel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class EmailActionController {

    // TODO: Apply IP/account rate limits before public release; cooldown alone cannot prevent distributed email spam.
    private final EmailActionService service;

    @PostMapping("/resend-verification")
    @Operation(
        summary = "Request another verification email",
        description = "Always returns the same accepted payload; requests within the cooldown do not send another email"
    )
    public ResponseEntity<ApiResponse<EmailRequestResponse>> resend(
        @Valid @RequestBody EmailRequest request,
        @RequestHeader(value = "Accept-Language", defaultValue = "vi") String locale
    ) {
        return accepted(service.request(request.email(), ActionPurpose.VERIFY_EMAIL, locale));
    }

    @PostMapping("/verify-email")
    @Operation(
        summary = "Verify email with a single-use token",
        description = "Explicit POST confirmation; opening the email link cannot consume the token"
    )
    public ResponseEntity<Void> verify(@Valid @RequestBody ActionTokenRequest request) {
        service.verify(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Request a password reset email",
        description = "Same accepted payload for all emails; never exposes account existence or reset tokens"
    )
    public ResponseEntity<ApiResponse<EmailRequestResponse>> forgot(
        @Valid @RequestBody EmailRequest request,
        @RequestHeader(value = "Accept-Language", defaultValue = "vi") String locale
    ) {
        return accepted(service.request(request.email(), ActionPurpose.PASSWORD_RESET, locale));
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Reset password",
        description = "Consumes a single-use reset token, verifies email ownership and revokes all previous access/refresh sessions; does not auto-login"
    )
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ApiResponse<EmailRequestResponse>> accepted(EmailRequestResponse response) {
        return ResponseEntity.accepted().body(
            ApiResponse.success("If the account is eligible, an email will be sent", response)
        );
    }
}
