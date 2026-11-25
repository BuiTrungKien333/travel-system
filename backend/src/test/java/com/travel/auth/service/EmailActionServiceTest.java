package com.travel.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.travel.auth.config.AuthEmailProperties;
import com.travel.auth.entity.*;
import com.travel.auth.mail.AuthEmail;
import com.travel.auth.repository.*;
import com.travel.auth.token.ActionTokenCodec;
import com.travel.common.exception.*;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailActionServiceTest {

    @Mock
    UserRepository users;

    @Mock
    ActionTokenRepository tokens;

    @Mock
    RefreshTokenRepository refreshTokens;

    @Mock
    PasswordEncoder passwords;

    @Mock
    ApplicationEventPublisher events;

    private final Instant now = Instant.parse("2026-10-01T00:00:00Z");
    private final UUID id = UUID.randomUUID();
    private final String raw = "a".repeat(43);
    private EmailActionService service;
    private User user;

    private AuthEmailProperties properties(boolean enabled) {
        return new AuthEmailProperties(
            enabled,
            "no-reply@example.com",
            URI.create("https://app.example.com/verify-email"),
            URI.create("https://app.example.com/reset-password"),
            Duration.ofHours(24),
            Duration.ofMinutes(30),
            Duration.ofSeconds(60),
            false
        );
    }

    @BeforeEach
    void setup() {
        service = new EmailActionService(
            users,
            tokens,
            refreshTokens,
            passwords,
            properties(true),
            events,
            Clock.fixed(now, ZoneOffset.UTC)
        );
        user = User.builder().email("an@example.com").fullName("An").passwordHash("old.hash").build();
        ReflectionTestUtils.setField(user, "id", id);
    }

    private void existing(ActionPurpose purpose) {
        when(users.lockByEmail("an@example.com")).thenReturn(Optional.of(user));
    }

    private void redeem(ActionPurpose purpose) {
        when(tokens.findByTokenHashAndPurpose(ActionTokenCodec.hash(raw), purpose)).thenReturn(
            Optional.of(new ActionToken(id, purpose, ActionTokenCodec.hash(raw), now, now.plusSeconds(60)))
        );
        when(users.lockById(id)).thenReturn(Optional.of(user));
    }

    private void invalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(AppException.class, error ->
            assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_ACTION_TOKEN)
        );
    }

    @Test
    void verificationStoresOnlyHashAndPublishesViEventWithConfiguredExpiry() {
        existing(ActionPurpose.VERIFY_EMAIL);
        var result = service.request(" AN@example.COM ", ActionPurpose.VERIFY_EMAIL, "vi");
        ArgumentCaptor<ActionToken> stored = ArgumentCaptor.forClass(ActionToken.class);
        ArgumentCaptor<AuthEmail> event = ArgumentCaptor.forClass(AuthEmail.class);
        verify(tokens).save(stored.capture());
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(stored.getValue().getTokenHash()).isEqualTo(ActionTokenCodec.hash(event.getValue().token()));
        assertThat(stored.getValue().getExpiresAt()).isEqualTo(now.plus(Duration.ofHours(24)));
        assertThat(result.resendAfterSeconds()).isEqualTo(60);
        assertThat(event.getValue().locale()).isEqualTo("vi");
        assertThat(event.getValue().toString()).doesNotContain(raw, user.getEmail());
    }

    @Test
    void resetUsesSeparatePurposeAndEnglishLocale() {
        existing(ActionPurpose.PASSWORD_RESET);
        service.request(user.getEmail(), ActionPurpose.PASSWORD_RESET, "en-US");
        ArgumentCaptor<AuthEmail> event = ArgumentCaptor.forClass(AuthEmail.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().purpose()).isEqualTo(ActionPurpose.PASSWORD_RESET);
        assertThat(event.getValue().expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
        assertThat(event.getValue().locale()).isEqualTo("en");
    }

    @Test
    void missingAccountReturnsGenericResponseWithoutCreatingTokensOrEmails() {
        assertThat(
            service.request("missing@example.com", ActionPurpose.PASSWORD_RESET, "vi").resendAfterSeconds()
        ).isEqualTo(60);
        verifyNoInteractions(tokens, events, passwords, refreshTokens);
    }

    @Test
    void disabledAccountCannotReceiveReset() {
        existing(ActionPurpose.PASSWORD_RESET);
        user.softDelete();
        assertThat(service.request(user.getEmail(), ActionPurpose.PASSWORD_RESET, "vi").resendAfterSeconds()).isEqualTo(
            60
        );
        verifyNoInteractions(tokens, events);
    }

    @Test
    void alreadyVerifiedAccountCannotReceiveVerification() {
        existing(ActionPurpose.VERIFY_EMAIL);
        user.setEmailVerifiedAt(now);
        service.request(user.getEmail(), ActionPurpose.VERIFY_EMAIL, "vi");
        verifyNoInteractions(tokens, events);
    }

    @Test
    void cooldownKeepsExistingTokenValidAndDoesNotSendAgain() {
        existing(ActionPurpose.VERIFY_EMAIL);
        when(tokens.findByUserIdAndPurpose(id, ActionPurpose.VERIFY_EMAIL)).thenReturn(
            Optional.of(
                new ActionToken(id, ActionPurpose.VERIFY_EMAIL, "old.hash", now.minusSeconds(59), now.plusSeconds(100))
            )
        );
        service.request(user.getEmail(), ActionPurpose.VERIFY_EMAIL, "vi");
        verify(tokens, never()).revoke(any(), any());
        verify(tokens, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void cooldownBoundaryReplacesToken() {
        existing(ActionPurpose.VERIFY_EMAIL);
        when(tokens.findByUserIdAndPurpose(id, ActionPurpose.VERIFY_EMAIL)).thenReturn(
            Optional.of(
                new ActionToken(id, ActionPurpose.VERIFY_EMAIL, "old.hash", now.minusSeconds(60), now.plusSeconds(100))
            )
        );
        service.request(user.getEmail(), ActionPurpose.VERIFY_EMAIL, "vi");
        verify(tokens).revoke(id, ActionPurpose.VERIFY_EMAIL);
        verify(tokens).save(any());
        verify(events).publishEvent(any(AuthEmail.class));
    }

    @Test
    void disabledMailServiceFailsBeforeLookingUpAnyAccount() {
        service = new EmailActionService(
            users,
            tokens,
            refreshTokens,
            passwords,
            properties(false),
            events,
            Clock.fixed(now, ZoneOffset.UTC)
        );
        assertThatThrownBy(() ->
            service.request(user.getEmail(), ActionPurpose.PASSWORD_RESET, "vi")
        ).isInstanceOfSatisfying(AppException.class, error ->
            assertThat(error.getErrorCode()).isEqualTo(ErrorCode.EMAIL_SERVICE_UNAVAILABLE)
        );
        verifyNoInteractions(users, tokens, events);
    }

    @Test
    void verificationRequiresSuccessfulAtomicConsumeAndMarksEmailVerified() {
        redeem(ActionPurpose.VERIFY_EMAIL);
        when(tokens.consume(ActionTokenCodec.hash(raw), ActionPurpose.VERIFY_EMAIL, now)).thenReturn(1);
        service.verify(raw);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(now);
        verifyNoInteractions(passwords, refreshTokens);
    }

    @Test
    void expiredReusedOrConcurrentLosingVerificationCannotMutateUser() {
        redeem(ActionPurpose.VERIFY_EMAIL);
        invalid(() -> service.verify(raw));
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void missingOrWrongPurposeTokenIsRejectedBeforeLockingUser() {
        invalid(() -> service.resetPassword(raw, "Password123@"));
        verifyNoInteractions(users, passwords, refreshTokens);
    }

    @Test
    void deletedUserCannotRedeemVerification() {
        redeem(ActionPurpose.VERIFY_EMAIL);
        user.softDelete();
        invalid(() -> service.verify(raw));
        verify(tokens, never()).consume(anyString(), any(), any());
    }

    @Test
    void resetHashesPasswordVerifiesOwnershipIncrementsVersionAndRevokesEverySessionAndActionToken() {
        redeem(ActionPurpose.PASSWORD_RESET);
        when(tokens.consume(ActionTokenCodec.hash(raw), ActionPurpose.PASSWORD_RESET, now)).thenReturn(1);
        when(passwords.encode("NewPassword123@")).thenReturn("new.hash");
        service.resetPassword(raw, "NewPassword123@");
        assertThat(user.getPasswordHash()).isEqualTo("new.hash");
        assertThat(user.getSessionVersion()).isEqualTo(1);
        assertThat(user.getEmailVerifiedAt()).isEqualTo(now);
        verify(refreshTokens).revokeAllForUser(id);
        verify(tokens).revokeAll(id);
    }

    @Test
    void losingResetCannotChangePasswordOrRevokeSessions() {
        redeem(ActionPurpose.PASSWORD_RESET);
        invalid(() -> service.resetPassword(raw, "NewPassword123@"));
        assertThat(user.getPasswordHash()).isEqualTo("old.hash");
        assertThat(user.getSessionVersion()).isZero();
        verifyNoInteractions(passwords, refreshTokens);
    }

    @Test
    void insecureLinkConfigurationIsRejectedOutsideDev() {
        assertThatThrownBy(() ->
            new AuthEmailProperties(
                true,
                "sender@example.com",
                URI.create("http://app.example.com/verify-email"),
                URI.create("https://app.example.com/reset-password"),
                Duration.ofHours(24),
                Duration.ofMinutes(30),
                Duration.ofSeconds(60),
                false
            )
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
