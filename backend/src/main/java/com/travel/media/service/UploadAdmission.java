package com.travel.media.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.config.MediaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UploadAdmission {
    private final MediaProperties limits;
    private final Clock clock;
    // shortcut: admission limits apply per process; use a shared limiter before running multiple replicas.
    private final Map<UUID, Window> users = new HashMap<>();
    private int active;
    private long nextSweep;
    private static class Window { long started; int attempts; int active; Window(long now) { started = now; } }

    public synchronized Permit acquire(UUID userId) {
        long now = clock.millis();
        if (now >= nextSweep) {
            users.values().removeIf(w -> w.active == 0 && now - w.started >= 60_000);
            nextSweep = now + 60_000;
        }
        if (!users.containsKey(userId) && users.size() >= 10_000) rejected();
        Window user = users.computeIfAbsent(userId, ignored -> new Window(now));
        if (now - user.started >= 60_000) { user.started = now; user.attempts = 0; }
        if (user.attempts >= limits.getUploadsPerMinute() || user.active >= limits.getConcurrentUploadsPerUser()
                || active >= limits.getConcurrentUploads()) rejected();
        user.attempts++; user.active++; active++;
        return new Permit(user);
    }

    private void rejected() { throw new AppException(ErrorCode.UPLOAD_RATE_LIMITED); }

    public final class Permit implements AutoCloseable {
        private final Window user;
        private boolean closed;
        private Permit(Window user) { this.user = user; }
        @Override public void close() {
            synchronized (UploadAdmission.this) {
                if (!closed) { user.active--; active--; closed = true; }
            }
        }
    }
}
