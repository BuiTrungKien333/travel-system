package com.travel.auth.service;

import com.travel.auth.config.*;
import com.travel.auth.dto.request.GoogleLoginRequest;
import com.travel.auth.dto.response.*;
import com.travel.auth.entity.*;
import com.travel.auth.mail.AuthEmail;
import com.travel.auth.repository.*;
import com.travel.auth.token.ActionTokenCodec;
import com.travel.common.exception.*;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.time.*;
import java.util.*;

@Service
public class GoogleAuthService {

    private final GoogleIdTokenVerifier verifier;
    private final GoogleAuthProperties properties;
    private final AuthEmailProperties emailProperties;
    private final UserRepository users;
    private final GoogleChallengeRepository challenges;
    private final GoogleLinkTicketRepository links;
    private final ActionTokenRepository actions;
    private final AuthTokenIssuer issuer;
    private final EmailActionService emailActions;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public GoogleAuthService(GoogleIdTokenVerifier verifier, GoogleAuthProperties properties, AuthEmailProperties emailProperties,
                             UserRepository users, GoogleChallengeRepository challenges, GoogleLinkTicketRepository links,
                             ActionTokenRepository actions, AuthTokenIssuer issuer, EmailActionService emailActions,
                             ApplicationEventPublisher events, Clock clock, PlatformTransactionManager manager) {
        this.verifier = verifier;
        this.properties = properties;
        this.emailProperties = emailProperties;
        this.users = users;
        this.challenges = challenges;
        this.links = links;
        this.actions = actions;
        this.issuer = issuer;
        this.emailActions = emailActions;
        this.events = events;
        this.clock = clock;
        this.transaction = new TransactionTemplate(manager);
    }

    @Transactional
    public GoogleChallengeResponse challenge() {
        configured();
        Instant now = clock.instant();
        challenges.deleteExpired(now);
        links.deleteExpired(now);
        String nonce = ActionTokenCodec.hash(ActionTokenCodec.generate());
        GoogleChallenge challenge = challenges.save(new GoogleChallenge(ActionTokenCodec.hash(nonce), now.plus(properties.challengeTtl())));
        return new GoogleChallengeResponse(challenge.getId(), nonce, properties.webClientId(), properties.challengeTtl().toMillis());
    }

    public GoogleLoginResponse login(GoogleLoginRequest request, String locale) {
        configured();
        var challenge = challenges.findById(request.challengeId()).filter(c -> c.getExpiresAt().isAfter(clock.instant()))
                .orElseThrow(this::invalid);
        // Google network/key verification occurs before opening the user-writing transaction.
        var identity = verifier.verify(request.idToken(), challenge.getNonceHash());

        try {
            return Objects.requireNonNull(transaction.execute(status -> complete(request.challengeId(), identity, locale)));
        } catch (DataIntegrityViolationException exception) {
            if (!accountConstraint(exception)) throw exception;
            // Concurrent first logins can see no user row. Retry once in a NEW transaction after rollback.
            return Objects.requireNonNull(transaction.execute(status -> complete(request.challengeId(), identity, locale)));
        }
    }

    private GoogleLoginResponse complete(UUID challengeId, GoogleIdTokenVerifier.Identity identity, String locale) {
        if (challenges.consume(challengeId, clock.instant()) != 1)
            throw invalid();
        var linked = users.lockByGoogleSubject(identity.subject());

        if (linked.isPresent()) {
            User user = linked.get();
            active(user);
            if (!user.isEmailVerified())
                throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);

            return GoogleLoginResponse.authenticated(issuer.issue(user));
        }

        var existing = users.lockByEmail(identity.email());
        User user;
        if (existing.isPresent()) {
            user = existing.get();
            active(user);
            if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(identity.subject()))
                throw conflict();
        } else {
            if (!identity.authoritativeEmail()) emailActions.ensureConfigured();
            user = users.saveAndFlush(User.builder().email(identity.email()).fullName(displayName(identity))
                    .role(Role.USER).avatarUrl(picture(identity.picture())).build());
        }

        if (!identity.authoritativeEmail())
            return requestEmailLink(user, identity.subject(), locale);

        // Google is authoritative for Gmail/Workspace: link automatically without asking for a password.
        // An unfinished password registration may have been planted by someone else: remove that password.
        if (!user.isEmailVerified()) {
            user.setPasswordHash(null);
            user.setEmailVerifiedAt(clock.instant());
            user.setSessionVersion(Math.incrementExact(user.getSessionVersion()));
            actions.revokeAll(user.getId());
        }
        user.setGoogleSubject(identity.subject());
        links.revokeForSubject(identity.subject());
        users.flush();
        return GoogleLoginResponse.authenticated(issuer.issue(user));
    }

    private GoogleLoginResponse requestEmailLink(User user, String subject, String locale) {
        emailActions.ensureConfigured();
        if (properties.linkUrl().getHost() != null && properties.linkUrl().getHost().endsWith(".invalid"))
            throw new AppException(ErrorCode.GOOGLE_NOT_CONFIGURED);

        Instant now = clock.instant();
        var previous = links.findByUserIdAndGoogleSubject(user.getId(), subject);
        if (previous.isEmpty() || !previous.get().getCreatedAt().plus(emailProperties.resendCooldown()).isAfter(now)) {
            links.revokeForSubject(subject);
            String raw = ActionTokenCodec.generate();
            Instant expiry = now.plus(properties.linkTtl());
            links.save(new GoogleLinkTicket(user.getId(), subject, user.getEmail(), ActionTokenCodec.hash(raw),
                    user.getSessionVersion(), now, expiry));

            events.publishEvent(new AuthEmail(user.getEmail(), raw, ActionPurpose.GOOGLE_LINK,
                    locale != null && locale.toLowerCase(Locale.ROOT).startsWith("en") ? "en" : "vi", expiry, properties.linkUrl()));
        }

        return GoogleLoginResponse.verification(user.getEmail(), emailProperties.resendCooldown().toSeconds());
    }

    @Transactional
    public AuthResponse verifyLink(String rawToken) {
        configured();
        String hash = ActionTokenCodec.hash(rawToken);
        var ticket = links.findByTokenHash(hash).orElseThrow(this::invalidLink);
        var user = users.lockById(ticket.getUserId()).orElseThrow(this::invalidLink);
        if (user.isDeleted() || user.getLockedAt() != null || user.getSessionVersion() != ticket.getSessionVersion() || !user.getEmail().equals(ticket.getEmail())
                || (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(ticket.getGoogleSubject()))
                || links.consume(hash, clock.instant()) != 1) throw invalidLink();

        if (!user.isEmailVerified()) {
            user.setPasswordHash(null); // Never activate an unverified password set by another person.
            user.setEmailVerifiedAt(clock.instant());
            user.setSessionVersion(Math.incrementExact(user.getSessionVersion()));
            actions.revokeAll(user.getId());
        }

        user.setGoogleSubject(ticket.getGoogleSubject());
        links.revokeForSubject(ticket.getGoogleSubject());
        try {
            users.flush();
        } catch (DataIntegrityViolationException exception) {
            if (accountConstraint(exception))
                throw conflict();

            throw exception;
        }
        return issuer.issue(user);
    }

    private void configured() {
        if (!properties.enabled())
            throw new AppException(ErrorCode.GOOGLE_NOT_CONFIGURED);
    }

    private void active(User user) {
        if (user.isDeleted() || user.getLockedAt() != null)
            throw new AppException(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
    }

    private AppException invalid() {
        return new AppException(ErrorCode.INVALID_GOOGLE_TOKEN);
    }

    private AppException invalidLink() {
        return new AppException(ErrorCode.INVALID_ACTION_TOKEN);
    }

    private AppException conflict() {
        return new AppException(ErrorCode.GOOGLE_ACCOUNT_CONFLICT);
    }

    private boolean accountConstraint(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && Set.of("uk_users_email", "uk_users_google_subject").contains(violation.getConstraintName()))
                return true;
        }

        return false;
    }

    private String displayName(GoogleIdTokenVerifier.Identity identity) {
        String value = identity.name() == null || identity.name().isBlank() ? identity.email().split("@", 2)[0] : identity.name().trim();
        return value.codePoints().limit(100).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
    }

    private String picture(String value) {
        if (value == null || value.length() > 1000) return null;
        try {
            var uri = URI.create(value);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null ? value : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
