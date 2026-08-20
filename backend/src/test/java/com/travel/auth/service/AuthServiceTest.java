package com.travel.auth.service;

import com.travel.auth.dto.request.LoginRequest;
import com.travel.auth.dto.request.RefreshTokenRequest;
import com.travel.auth.dto.request.RegisterRequest;
import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.service.impl.AuthServiceImpl;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.jwt.JwtUtils;
import com.travel.security.jwt.TokenType;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.RolePermissionRepository;
import com.travel.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Nested
    @DisplayName("register() tests")
    class RegisterTests {

        @Test
        @DisplayName("register with valid data should encode password, save user, and return tokens")
        void register_success() {
            RegisterRequest request = RegisterRequest.builder()
                    .fullName("Nguyen Van Test")
                    .email("user@travel.com")
                    .password("Password123@")
                    .build();

            when(userRepository.existsByEmail("user@travel.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123@")).thenReturn("hashed_password_123");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(rolePermissionRepository.findPermissionNamesByRole(Role.USER)).thenReturn(Set.of("trip:read", "trip:create"));
            when(jwtUtils.generateAccessToken(any(CustomUserDetails.class))).thenReturn("mock.access.token");
            when(jwtUtils.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("mock.refresh.token");
            when(jwtUtils.getJwtExpiration()).thenReturn(1800000L);

            AuthResponse response = authService.register(request);

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

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getEmail()).isEqualTo("user@travel.com");
            assertThat(capturedUser.getFullName()).isEqualTo("Nguyen Van Test");
            assertThat(capturedUser.getPasswordHash()).isEqualTo("hashed_password_123");
            assertThat(capturedUser.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("register should normalize email and full name by trimming whitespace")
        void register_normalization_success() {
            RegisterRequest request = RegisterRequest.builder()
                    .fullName("  Nguyen Van Test  ")
                    .email("  USER@Travel.COM  ")
                    .password("Password123@")
                    .build();

            when(userRepository.existsByEmail("user@travel.com")).thenReturn(false);
            when(passwordEncoder.encode("Password123@")).thenReturn("hashed_pwd");
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(rolePermissionRepository.findPermissionNamesByRole(Role.USER)).thenReturn(Set.of("trip:read"));
            when(jwtUtils.generateAccessToken(any())).thenReturn("token");
            when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh");
            when(jwtUtils.getJwtExpiration()).thenReturn(1800000L);

            authService.register(request);

            verify(userRepository).existsByEmail("user@travel.com");
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFullName()).isEqualTo("Nguyen Van Test");
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@travel.com");
        }

        @Test
        @DisplayName("register with existing email should throw AppException with USER_ALREADY_EXISTS")
        void register_emailAlreadyExists_throwsConflict() {
            RegisterRequest request = RegisterRequest.builder()
                    .fullName("Nguyen Van Test")
                    .email("user@travel.com")
                    .password("Password123@")
                    .build();

            when(userRepository.existsByEmail("user@travel.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
                    });

            verify(userRepository).existsByEmail("user@travel.com");
            verifyNoInteractions(passwordEncoder);
            verify(userRepository, never()).save(any());
            verifyNoInteractions(jwtUtils);
        }
    }

    @Nested
    @DisplayName("login() tests")
    class LoginTests {

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

    @Nested
    @DisplayName("refreshToken() tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("refreshToken with valid token should generate new tokens and return AuthResponse")
        void refreshToken_success() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("valid.refresh.token")
                    .build();

            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getSubject()).thenReturn("user@travel.com");

            when(jwtUtils.extractAllClaims("valid.refresh.token", TokenType.REFRESH_TOKEN)).thenReturn(mockClaims);
            when(userRepository.findByEmail("user@travel.com")).thenReturn(Optional.of(testUser));
            when(rolePermissionRepository.findPermissionNamesByRole(Role.USER)).thenReturn(Set.of("trip:read", "trip:create"));
            when(jwtUtils.generateAccessToken(any(CustomUserDetails.class))).thenReturn("new.access.token");
            when(jwtUtils.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("new.refresh.token");
            when(jwtUtils.getJwtExpiration()).thenReturn(1800000L);

            AuthResponse response = authService.refreshToken(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new.access.token");
            assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(1800000L);

            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getId()).isEqualTo(userId);
            assertThat(response.getUser().getEmail()).isEqualTo("user@travel.com");

            verify(jwtUtils).extractAllClaims("valid.refresh.token", TokenType.REFRESH_TOKEN);
            verify(userRepository).findByEmail("user@travel.com");
            verify(rolePermissionRepository).findPermissionNamesByRole(Role.USER);
        }

        @Test
        @DisplayName("refreshToken with invalid signature or expired token should throw INVALID_REFRESH_TOKEN")
        void refreshToken_invalidToken_throwsUnauthorized() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalid.refresh.token")
                    .build();

            when(jwtUtils.extractAllClaims("invalid.refresh.token", TokenType.REFRESH_TOKEN))
                    .thenThrow(new SignatureException("Invalid JWT signature"));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
                    });

            verify(jwtUtils).extractAllClaims("invalid.refresh.token", TokenType.REFRESH_TOKEN);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("refreshToken when user does not exist in DB should throw INVALID_REFRESH_TOKEN")
        void refreshToken_userNotFound_throwsUnauthorized() {
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("valid.refresh.token")
                    .build();

            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getSubject()).thenReturn("nonexistent@travel.com");

            when(jwtUtils.extractAllClaims("valid.refresh.token", TokenType.REFRESH_TOKEN)).thenReturn(mockClaims);
            when(userRepository.findByEmail("nonexistent@travel.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
                    });

            verify(userRepository).findByEmail("nonexistent@travel.com");
        }

        @Test
        @DisplayName("refreshToken with soft-deleted user should throw ACCOUNT_LOCKED_OR_DELETED")
        void refreshToken_softDeletedUser_throwsForbidden() {
            testUser.softDelete();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("valid.refresh.token")
                    .build();

            Claims mockClaims = mock(Claims.class);
            when(mockClaims.getSubject()).thenReturn("user@travel.com");

            when(jwtUtils.extractAllClaims("valid.refresh.token", TokenType.REFRESH_TOKEN)).thenReturn(mockClaims);
            when(userRepository.findByEmail("user@travel.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
                    });

            verify(userRepository).findByEmail("user@travel.com");
        }
    }
}
