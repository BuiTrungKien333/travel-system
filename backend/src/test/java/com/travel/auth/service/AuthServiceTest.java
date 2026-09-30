package com.travel.auth.service;

import com.travel.auth.dto.request.*;
import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.dto.response.RegistrationResponse;
import com.travel.auth.config.AuthEmailProperties;
import com.travel.auth.entity.RefreshToken;
import com.travel.auth.repository.RefreshTokenRepository;
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
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock RolePermissionRepository permissions;
    @Mock PasswordEncoder encoder;
    @Mock JwtUtils jwt;
    @Mock RefreshTokenRepository tokens;
    @Mock EmailActionService emailActions;
    @Mock AuthEmailProperties emailProperties;
    @InjectMocks AuthServiceImpl service;
    private User user;
    private UUID userId;

    @BeforeEach void setup() {
        userId = UUID.randomUUID();
        user = User.builder().email("user@travel.com").fullName("Nguyen Van Test")
                .passwordHash("hashed").role(Role.USER).emailVerifiedAt(Instant.now()).build();
        ReflectionTestUtils.setField(user, "id", userId);
    }

    private RegisterRequest registration() {
        return RegisterRequest.builder().fullName("  Nguyen Van Test  ")
                .email("  USER@Travel.COM  ").password("Password123@").build();
    }
    private LoginRequest login() {
        return LoginRequest.builder().email("  USER@Travel.COM  ").password("Password123@").build();
    }
    private RefreshTokenRequest refresh() {
        return RefreshTokenRequest.builder().refreshToken("valid.refresh").build();
    }
    private void issuance() {
        when(permissions.findPermissionNamesByRole(user.getRole())).thenReturn(Set.of("trip:read"));
        when(jwt.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access");
        when(jwt.generateRefreshToken(any(CustomUserDetails.class))).thenReturn("new.refresh");
        when(jwt.getJwtExpiration()).thenReturn(1800000L);
        when(jwt.getJwtRefreshExpiration()).thenReturn(604800000L);
    }
    private Claims validRefresh() {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.getSubject()).thenReturn(user.getEmail());
        when(jwt.extractAllClaims("valid.refresh", TokenType.REFRESH_TOKEN)).thenReturn(claims);
        return claims;
    }
    private void expectCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }

    @Test void registerPersistsUnverifiedUserAndRequestsEmailWithoutIssuingCredentials() {
        when(encoder.encode("Password123@")).thenReturn("hashed");
        when(users.saveAndFlush(any())).thenReturn(user);
        when(emailProperties.resendCooldown()).thenReturn(java.time.Duration.ofSeconds(60));
        RegistrationResponse response = service.register(registration(), "vi");
        ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captured.capture());
        assertThat(captured.getValue().getEmail()).isEqualTo("user@travel.com");
        assertThat(captured.getValue().getFullName()).isEqualTo("Nguyen Van Test");
        assertThat(captured.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captured.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(captured.getValue().isEmailVerified()).isFalse();
        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.resendAfterSeconds()).isEqualTo(60);
        verify(emailActions).sendInitialVerification(user, "vi");
        verifyNoInteractions(jwt, tokens, permissions);
    }
    @Test void existingEmailReturnsConflictWithoutIssuingTokens() {
        when(users.existsByEmail("user@travel.com")).thenReturn(true);
        expectCode(() -> service.register(registration(), "vi"), ErrorCode.USER_ALREADY_EXISTS);
        verifyNoInteractions(encoder, jwt, tokens);
    }
    @Test void concurrentDuplicateEmailMapsDatabaseConstraintToConflict() {
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate",
                new ConstraintViolationException("duplicate", new SQLException(), "uk_users_email")));
        expectCode(() -> service.register(registration(), "vi"), ErrorCode.USER_ALREADY_EXISTS);
        verifyNoInteractions(jwt, tokens);
    }
    @Test void unrelatedDatabaseConstraintIsNotMisreportedAsDuplicateEmail() {
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("other",
                new ConstraintViolationException("other", new SQLException(), "other_constraint")));
        assertThatThrownBy(() -> service.register(registration(), "vi")).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void loginReturnsTokensForCorrectCredentials() {
        when(users.lockByEmail("user@travel.com")).thenReturn(Optional.of(user));
        when(encoder.matches("Password123@", "hashed")).thenReturn(true);
        issuance();
        assertThat(service.login(login()).getUser().getId()).isEqualTo(userId);
        verify(tokens).save(any());
    }
    @Test void normalizationDoesNotDependOnTurkishSystemLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            expectCode(() -> service.login(LoginRequest.builder().email("INFO@TRAVEL.COM")
                    .password("Password123@").build()), ErrorCode.INVALID_CREDENTIALS);
            verify(users).lockByEmail("info@travel.com");
        } finally { Locale.setDefault(previous); }
    }
    @Test void missingUserStillPerformsBcryptAndReturnsSameCredentialError() {
        expectCode(() -> service.login(login()), ErrorCode.INVALID_CREDENTIALS);
        verify(encoder).matches(eq("Password123@"), startsWith("$2a$"));
        verifyNoInteractions(jwt, tokens);
    }
    @Test void wrongPasswordReturnsCredentialError() {
        when(users.lockByEmail("user@travel.com")).thenReturn(Optional.of(user));
        expectCode(() -> service.login(login()), ErrorCode.INVALID_CREDENTIALS);
        verifyNoInteractions(jwt, tokens);
    }
    @Test void disabledUserWithCorrectPasswordIsForbidden() {
        user.softDelete();
        when(users.lockByEmail("user@travel.com")).thenReturn(Optional.of(user));
        when(encoder.matches("Password123@", "hashed")).thenReturn(true);
        expectCode(() -> service.login(login()), ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
        verifyNoInteractions(jwt, tokens);
    }
    @Test void disabledUserWithWrongPasswordDoesNotRevealAccountStatus() {
        user.softDelete();
        when(users.lockByEmail("user@travel.com")).thenReturn(Optional.of(user));
        expectCode(() -> service.login(login()), ErrorCode.INVALID_CREDENTIALS);
    }
    @Test void refreshConsumesOldTokenAndPersistsReplacement() {
        validRefresh();
        when(users.lockById(userId)).thenReturn(Optional.of(user));
        when(tokens.consume(eq(AuthServiceImpl.hashToken("valid.refresh")), eq(userId), any())).thenReturn(1);
        issuance();
        assertThat(service.refreshToken(refresh()).getRefreshToken()).isEqualTo("new.refresh");
        InOrder order = inOrder(tokens);
        order.verify(tokens).consume(anyString(), eq(userId), any());
        order.verify(tokens).save(any());
    }
    @Test void invalidSignatureDoesNotReadDatabaseOrIssueTokens() {
        when(jwt.extractAllClaims("valid.refresh", TokenType.REFRESH_TOKEN))
                .thenThrow(new SignatureException("invalid"));
        expectCode(() -> service.refreshToken(refresh()), ErrorCode.INVALID_REFRESH_TOKEN);
        verifyNoInteractions(users, tokens);
    }
    @Test void missingUserIdClaimIsRejected() {
        when(jwt.extractAllClaims("valid.refresh", TokenType.REFRESH_TOKEN)).thenReturn(mock(Claims.class));
        expectCode(() -> service.refreshToken(refresh()), ErrorCode.INVALID_REFRESH_TOKEN);
        verifyNoInteractions(users, tokens);
    }
    @Test void unknownUserIsRejected() {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(jwt.extractAllClaims("valid.refresh", TokenType.REFRESH_TOKEN)).thenReturn(claims);
        expectCode(() -> service.refreshToken(refresh()), ErrorCode.INVALID_REFRESH_TOKEN);
        verifyNoInteractions(tokens);
    }
    @Test void refreshForDisabledUserIsForbidden() {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(jwt.extractAllClaims("valid.refresh", TokenType.REFRESH_TOKEN)).thenReturn(claims);
        when(users.lockById(userId)).thenReturn(Optional.of(user));
        user.softDelete();
        expectCode(() -> service.refreshToken(refresh()), ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
        verifyNoInteractions(tokens);
    }
    @Test void refreshSubjectMustMatchPersistedUser() {
        validRefresh();
        when(users.lockById(userId)).thenReturn(Optional.of(user));
        user.setEmail("other@travel.com");
        expectCode(() -> service.refreshToken(refresh()), ErrorCode.INVALID_REFRESH_TOKEN);
        verifyNoInteractions(tokens);
    }
    @Test void replayedExpiredOrConcurrentLosingTokenCannotIssueNewTokens() {
        validRefresh();
        when(users.lockById(userId)).thenReturn(Optional.of(user));
        expectCode(() -> service.refreshToken(refresh()), ErrorCode.INVALID_REFRESH_TOKEN);
        verify(tokens, never()).save(any());
        verify(jwt, never()).generateAccessToken(any());
    }
    @Test void logoutRevokesOnlySuppliedTokenAndIsIdempotent() {
        service.logout(refresh());
        service.logout(refresh());
        verify(tokens, times(2)).revoke(AuthServiceImpl.hashToken("valid.refresh"));
        verifyNoInteractions(jwt, users);
    }
}
