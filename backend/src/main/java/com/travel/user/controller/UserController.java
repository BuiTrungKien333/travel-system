package com.travel.user.controller;

import com.travel.common.response.ApiResponse;
import com.travel.common.response.ErrorResponse;
import com.travel.user.dto.response.UserProfileResponse;
import com.travel.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user profile and account")
@SecurityRequirement(name = "bearerAuth")
@Slf4j(topic = "USER_CONTROLLER")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get current user profile",
            description = "Retrieve detailed profile information of the currently authenticated user identified via JWT token"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT token",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Unauthorized Error",
                                    value = "{\"success\": false, \"code\": \"AUTH_001\", \"message\": \"Unauthorized access\", \"timestamp\": \"2026-08-25T12:00:00Z\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Account has been locked or deactivated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Forbidden Error",
                                    value = "{\"success\": false, \"code\": \"AUTH_007\", \"message\": \"Account has been locked or deactivated\", \"timestamp\": \"2026-08-25T12:00:00Z\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "User not found in the system",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Not Found Error",
                                    value = "{\"success\": false, \"code\": \"AUTH_006\", \"message\": \"User not found\", \"timestamp\": \"2026-08-25T12:00:00Z\"}"
                            )
                    )
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentProfile() {
        UserProfileResponse response = userService.getCurrentProfile();
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }
}
