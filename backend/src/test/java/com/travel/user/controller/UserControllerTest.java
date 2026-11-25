package com.travel.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.common.exception.GlobalExceptionHandler;
import com.travel.common.util.Translator;
import com.travel.user.dto.request.UpdateProfileRequest;
import com.travel.user.dto.response.UserProfileResponse;
import com.travel.user.enums.Role;
import com.travel.user.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @Mock
    private Translator translator;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
            .setControllerAdvice(new GlobalExceptionHandler(translator))
            .build();
    }

    // --- GET /api/v1/users/me tests ---

    @Test
    @DisplayName("GET /api/v1/users/me should return 200 OK and UserProfileResponse")
    void getCurrentProfile_success_returns200Ok() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = UserProfileResponse.builder()
            .id(userId)
            .email("user@travel.com")
            .fullName("Nguyen Van A")
            .role(Role.USER)
            .avatarUrl("https://example.com/avatar.png")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(userService.getCurrentProfile()).thenReturn(response);

        mockMvc
            .perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.email").value("user@travel.com"))
            .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/avatar.png"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 401 Unauthorized when unauthenticated")
    void getCurrentProfile_unauthenticated_returns401() throws Exception {
        when(userService.getCurrentProfile()).thenThrow(new AppException(ErrorCode.UNAUTHORIZED));

        mockMvc
            .perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 403 Forbidden when account is locked or deleted")
    void getCurrentProfile_accountDeleted_returns403() throws Exception {
        when(userService.getCurrentProfile()).thenThrow(new AppException(ErrorCode.ACCOUNT_LOCKED_OR_DELETED));

        mockMvc
            .perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("AUTH_007"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 404 Not Found when user is not found")
    void getCurrentProfile_userNotFound_returns404() throws Exception {
        when(userService.getCurrentProfile()).thenThrow(new AppException(ErrorCode.USER_NOT_FOUND));

        mockMvc
            .perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    // --- PUT /api/v1/users/me tests ---

    @Test
    @DisplayName("PUT /api/v1/users/me with valid body should return 200 OK and updated UserProfileResponse")
    void updateProfile_validRequest_returns200Ok() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .fullName("Nguyen Van B")
            .avatarUrl("https://example.com/new-avatar.jpg")
            .build();

        UserProfileResponse response = UserProfileResponse.builder()
            .id(userId)
            .email("user@travel.com")
            .fullName("Nguyen Van B")
            .role(Role.USER)
            .avatarUrl("https://example.com/new-avatar.jpg")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc
            .perform(
                put("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Profile updated successfully"))
            .andExpect(jsonPath("$.data.fullName").value("Nguyen Van B"))
            .andExpect(jsonPath("$.data.avatarUrl").value("https://example.com/new-avatar.jpg"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me with blank fullName should return 400 Bad Request")
    void updateProfile_blankFullName_returns400BadRequest() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
            .fullName("   ")
            .avatarUrl("https://example.com/avatar.jpg")
            .build();

        mockMvc
            .perform(
                put("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COMMON_003"))
            .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me with fullName exceeding 100 chars should return 400 Bad Request")
    void updateProfile_fullNameTooLong_returns400BadRequest() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder().fullName("A".repeat(101)).build();

        mockMvc
            .perform(
                put("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("COMMON_003"))
            .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me should return 401 Unauthorized when unauthenticated")
    void updateProfile_unauthenticated_returns401() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder().fullName("Nguyen Van B").build();

        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenThrow(
            new AppException(ErrorCode.UNAUTHORIZED)
        );

        mockMvc
            .perform(
                put("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("AUTH_001"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me should return 403 Forbidden when account is locked or deleted")
    void updateProfile_accountDeleted_returns403() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder().fullName("Nguyen Van B").build();

        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenThrow(
            new AppException(ErrorCode.ACCOUNT_LOCKED_OR_DELETED)
        );

        mockMvc
            .perform(
                put("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("AUTH_007"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me should return 404 Not Found when user is not found")
    void updateProfile_userNotFound_returns404() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder().fullName("Nguyen Van B").build();

        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenThrow(
            new AppException(ErrorCode.USER_NOT_FOUND)
        );

        mockMvc
            .perform(
                put("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("AUTH_006"));
    }
}
