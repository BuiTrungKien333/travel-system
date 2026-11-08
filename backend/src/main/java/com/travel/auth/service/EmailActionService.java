package com.travel.auth.service;

import com.travel.auth.config.AuthEmailProperties;
import com.travel.auth.dto.response.EmailRequestResponse;
import com.travel.auth.entity.*;
import com.travel.auth.mail.AuthEmail;
import com.travel.auth.repository.*;
import com.travel.auth.token.ActionTokenCodec;
import com.travel.common.exception.*;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailActionService {

    private final UserRepository users;
    private final ActionTokenRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final AuthEmailProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public void ensureConfigured() {
        if (!properties.emailEnabled()) throw new AppException(ErrorCode.EMAIL_SERVICE_UNAVAILABLE);
    }

    /**
     * Called inside registration's transaction, which already owns the new user row.
     */
    public void sendInitialVerification(User user, String locale) {
        ensureConfigured();
        issue(user, ActionPurpose.VERIFY_EMAIL, locale);
    }

    @Transactional
    public EmailRequestResponse request(String email, ActionPurpose purpose, String locale) {
        ensureConfigured(); // Same failure when email delivery is disabled, regardless of account existence.
        // User-row lock serializes requests, redemption, login and reset across all server instances.
        users.lockByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(user -> !user.isDeleted())
                .filter(user -> purpose == ActionPurpose.PASSWORD_RESET || !user.isEmailVerified())
                .ifPresent(user -> issue(user, purpose, locale));

        return new EmailRequestResponse(properties.resendCooldown().toSeconds());
    }

    private void issue(User user, ActionPurpose purpose, String locale) {
        Instant now = clock.instant();
        var previous = tokens.findByUserIdAndPurpose(user.getId(), purpose);
        if (previous.isPresent() && previous.get().getCreatedAt().plus(properties.resendCooldown()).isAfter(now))
            return;

        tokens.revoke(user.getId(), purpose);
        String token = ActionTokenCodec.generate();
        Instant expiry = now.plus(purpose == ActionPurpose.VERIFY_EMAIL ? properties.verificationTtl() : properties.resetTtl());
        tokens.save(new ActionToken(user.getId(), purpose, ActionTokenCodec.hash(token), now, expiry));
        events.publishEvent(new AuthEmail(user.getEmail(), token, purpose,
                locale != null && locale.toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "vi", expiry));
    }

    @Transactional
    public void verify(String rawToken) {
        User user = consume(rawToken, ActionPurpose.VERIFY_EMAIL);
        if (user.isEmailVerified())
            throw invalidToken();

        user.setEmailVerifiedAt(clock.instant());
    }

    @Transactional
    public void resetPassword(String rawToken, String password) {
        User user = consume(rawToken, ActionPurpose.PASSWORD_RESET);
        user.setPasswordHash(passwords.encode(password));
        // Email ownership was proven by this token, including for an unfinished registration.
        if (!user.isEmailVerified())
            user.setEmailVerifiedAt(clock.instant());

        user.setSessionVersion(Math.incrementExact(user.getSessionVersion()));
        refreshTokens.revokeAllForUser(user.getId());
        tokens.revokeAll(user.getId());
    }

    private User consume(String rawToken, ActionPurpose purpose) {
        String hash = ActionTokenCodec.hash(rawToken);
        ActionToken token = tokens.findByTokenHashAndPurpose(hash, purpose)
                .orElseThrow(this::invalidToken);

        User user = users.lockById(token.getUserId()).orElseThrow(this::invalidToken);

        if (user.isDeleted() || tokens.consume(hash, purpose, clock.instant()) != 1)
            throw invalidToken();

        return user;
    }

    private AppException invalidToken() {
        return new AppException(ErrorCode.INVALID_ACTION_TOKEN);
    }
}
