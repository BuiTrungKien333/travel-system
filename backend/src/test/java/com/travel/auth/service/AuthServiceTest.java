package com.travel.auth.service;

import com.travel.auth.dto.request.LoginRequest;
import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.service.impl.AuthServiceImpl;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.jwt.JwtUtils;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.RolePermissionRepository;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .email("user@travel.com")
                .fullName("Nguyen Van Test")
                .passwordHash("hashed_password_123")
                .role(Role.USER)
                .avatarUrl("https://example.com/avatar.jpg")
                .build();
        ReflectionTestUtils.setField(testUser, "id", userId);
    }

    @Test
    @DisplayName("login with valid email and password should return AuthResponse with tokens and user info")
    void login_success() {
        LoginRequest request = LoginRequest.builder()
                .email("user@travel.com")
                .password("Password123@")
                .build();

        when(userRepository.findByEmail("user@travel.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123@", "hashed_password_123")).thenReturn(true);
        when(rolePermissionRepository.findPermissionNamesByRole(Role.USER)).thenReturn(Set.of("trip:read", "trip:create"));
        when(jwtUtils.generateAccessToken(any(CustomUserDetails.class))).thenReturn("mock.access.token");
        when(jwtUtils.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("mock.refresh.token");
        when(jwtUtils.getJwtExpiration()).thenReturn(1800000L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(1800000L);

        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getEmail()).isEqualTo("user@travel.com");
        assertThat(response.getUser().getFullName()).isEqualTo("Nguyen Van Test");
        assertThat(response.getUser().getRole()).isEqualTo(Role.USER);

        verify(userRepository).findByEmail("user@travel.com");
        verify(passwordEncoder).matches("Password123@", "hashed_password_123");
        verify(rolePermissionRepository).findPermissionNamesByRole(Role.USER);
    }

    @Test
    @DisplayName("login should normalize email by trimming and converting to lowercase")
    void login_emailNormalization_success() {
        LoginRequest request = LoginRequest.builder()
                .email("  USER@Travel.COM  ")
                .password("Password123@")
                .build();

        when(userRepository.findByEmail("user@travel.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password123@", "hashed_password_123")).thenReturn(true);
        when(rolePermissionRepository.findPermissionNamesByRole(Role.USER)).thenReturn(Set.of("trip:read"));
        when(jwtUtils.generateAccessToken(any())).thenReturn("token");
        when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtUtils.getJwtExpiration()).thenReturn(1800000L);

        authService.login(request);

        verify(userRepository).findByEmail("user@travel.com");
    }

    @Test
    @DisplayName("login with non-existing email should throw AppException with INVALID_CREDENTIALS")
    void login_userNotFound_throwsUnauthorized() {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@travel.com")
                .password("Password123@")
                .build();

        when(userRepository.findByEmail("nonexistent@travel.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
                });

        verify(userRepository).findByEmail("nonexistent@travel.com");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("login with incorrect password should throw AppException with INVALID_CREDENTIALS")
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest request = LoginRequest.builder()
                .email("user@travel.com")
                .password("WrongPassword123")
                .build();

        when(userRepository.findByEmail("user@travel.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword123", "hashed_password_123")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
                });

        verify(userRepository).findByEmail("user@travel.com");
        verify(passwordEncoder).matches("WrongPassword123", "hashed_password_123");
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("login with soft-deleted account should throw AppException with ACCOUNT_LOCKED_OR_DELETED")
    void login_softDeletedUser_throwsForbidden() {
        testUser.softDelete();

        LoginRequest request = LoginRequest.builder()
                .email("user@travel.com")
                .password("Password123@")
                .build();

        when(userRepository.findByEmail("user@travel.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException appEx = (AppException) e;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
                });

        verify(userRepository).findByEmail("user@travel.com");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtUtils);
    }
}
