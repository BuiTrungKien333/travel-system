package com.travel.user.service;

import com.travel.auth.repository.RefreshTokenRepository;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.util.SecurityUtils;
import com.travel.user.enums.Role;
import com.travel.user.repository.UserRepository;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:users')")
public class AdminAccountService {

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;

    public record Account(
        UUID id,
        String fullName,
        String email,
        Role role,
        boolean locked,
        boolean verified,
        long version,
        Instant createdAt
    ) {}

    public record Page(List<Account> content, int page, int size, long totalElements) {}

    public record Change(@NotNull Role role, @NotNull Boolean locked, @NotNull @PositiveOrZero Long version) {}

    public record Event(
        UUID id,
        UUID actorId,
        UUID targetId,
        String actorEmail,
        String targetEmail,
        String oldRole,
        String newRole,
        boolean wasLocked,
        boolean isLocked,
        Instant createdAt
    ) {}

    public record Events(List<Event> content, int page, int size, long totalElements) {}

    @Transactional(readOnly = true)
    public Page list(String query, int page, int size) {
        validatePage(page, size);
        String text = query == null ? "" : query.trim();
        if (text.length() > 120) throw new AppException(ErrorCode.VALIDATION_ERROR);
        String pattern =
            "%" +
            text.toLowerCase(java.util.Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") +
            "%";
        String filter = " WHERE deleted_at IS NULL AND (lower(email) LIKE ? OR lower(full_name) LIKE ?)";
        long total = jdbc.queryForObject("SELECT count(*) FROM users" + filter, Long.class, pattern, pattern);
        var content = jdbc.query(
            "SELECT id,full_name,email,role,locked_at,email_verified_at,session_version,created_at FROM users" +
                filter +
                " ORDER BY created_at DESC,id LIMIT ? OFFSET ?",
            (rs, n) ->
                new Account(
                    rs.getObject("id", UUID.class),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    Role.valueOf(rs.getString("role")),
                    rs.getTimestamp("locked_at") != null,
                    rs.getTimestamp("email_verified_at") != null,
                    rs.getLong("session_version"),
                    rs.getTimestamp("created_at").toInstant()
                ),
            pattern,
            pattern,
            size,
            (long) page * size
        );
        return new Page(content, page, size, total);
    }

    @Transactional
    public void change(UUID targetId, Change request) {
        // Serialize account administration so concurrent demotions cannot remove the last active admin.
        jdbc.execute("SELECT pg_advisory_xact_lock(72419022)");
        UUID actorId = SecurityUtils.getCurrentUserId();
        var actor = users.lockById(actorId).orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN));
        if (
            actor.isDeleted() ||
            actor.getLockedAt() != null ||
            actor.getRole() != Role.ADMIN ||
            !actor.isEmailVerified()
        ) throw new AppException(ErrorCode.FORBIDDEN);
        var target = users
            .lockById(targetId)
            .filter(u -> !u.isDeleted())
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (target.getSessionVersion() != request.version()) throw new AppException(ErrorCode.ADMIN_ACCOUNT_CONFLICT);
        if (target.getRole() == request.role() && (target.getLockedAt() != null) == request.locked()) return;
        if (actorId.equals(targetId)) throw new AppException(ErrorCode.ADMIN_SELF_CHANGE);
        if (request.role() == Role.ADMIN && !target.isEmailVerified()) throw new AppException(
            ErrorCode.ADMIN_UNVERIFIED
        );
        boolean wasLocked = target.getLockedAt() != null;
        if (
            target.getRole() == Role.ADMIN &&
            !wasLocked &&
            target.isEmailVerified() &&
            (request.role() != Role.ADMIN || request.locked()) &&
            jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE role='ADMIN' AND locked_at IS NULL AND deleted_at IS NULL AND email_verified_at IS NOT NULL",
                Long.class
            ) <= 1
        ) throw new AppException(ErrorCode.ADMIN_LAST_ACCOUNT);
        jdbc.update(
            "INSERT INTO admin_account_events(actor_id,target_id,old_role,new_role,was_locked,is_locked) VALUES (?,?,?,?,?,?)",
            actorId,
            targetId,
            target.getRole().name(),
            request.role().name(),
            wasLocked,
            request.locked()
        );
        target.setRole(request.role());
        target.setLockedAt(request.locked() ? Instant.now() : null);
        target.setSessionVersion(Math.incrementExact(target.getSessionVersion()));
        refreshTokens.revokeAllForUser(targetId);
        users.flush();
    }

    @Transactional(readOnly = true)
    public Events events(int page, int size) {
        validatePage(page, size);
        long total = jdbc.queryForObject("SELECT count(*) FROM admin_account_events", Long.class);
        var content = jdbc.query(
            "SELECT e.*,actor.email AS actor_email,target.email AS target_email FROM admin_account_events e JOIN users actor ON actor.id=e.actor_id JOIN users target ON target.id=e.target_id ORDER BY e.created_at DESC,e.id LIMIT ? OFFSET ?",
            (rs, n) ->
                new Event(
                    rs.getObject("id", UUID.class),
                    rs.getObject("actor_id", UUID.class),
                    rs.getObject("target_id", UUID.class),
                    rs.getString("actor_email"),
                    rs.getString("target_email"),
                    rs.getString("old_role"),
                    rs.getString("new_role"),
                    rs.getBoolean("was_locked"),
                    rs.getBoolean("is_locked"),
                    rs.getTimestamp("created_at").toInstant()
                ),
            size,
            (long) page * size
        );
        return new Events(content, page, size, total);
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new AppException(ErrorCode.VALIDATION_ERROR);
    }
}
