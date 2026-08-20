package com.travel.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.auth.dto.request.LoginRequest;
import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.service.AuthService;
import com.travel.common.exception.GlobalExceptionHandler;
import com.travel.common.util.Translator;
import com.travel.user.dto.response.UserSummaryResponse;
import com.travel.user.enums.Role;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private Translator translator;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler(translator))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid body should return 200 OK and AuthResponse")
    void login_validRequest_returns200Ok() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@travel.com")
                .password("Password123@")
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .accessToken("mock.access.token")
                .refreshToken("mock.refresh.token")
                .tokenType("Bearer")
                .expiresIn(1800000L)
                .user(UserSummaryResponse.builder()
                        .id(UUID.randomUUID())
                        .email("user@travel.com")
                        .fullName("Test User")
                        .role(Role.USER)
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("mock.access.token"))
                .andExpect(jsonPath("$.data.refreshToken").value("mock.refresh.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("user@travel.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with blank email should return 400 Bad Request")
    void login_blankEmail_returns400BadRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("")
                .password("Password123@")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid email format should return 400 Bad Request")
    void login_invalidEmailFormat_returns400BadRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("invalid-email-format")
                .password("Password123@")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with blank password should return 400 Bad Request")
    void login_blankPassword_returns400BadRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@travel.com")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_003"))
                .andExpect(jsonPath("$.errors.password").exists());
    }
}
