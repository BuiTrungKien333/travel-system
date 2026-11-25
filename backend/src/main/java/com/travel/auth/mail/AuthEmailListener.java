package com.travel.auth.mail;

import java.time.Clock;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class AuthEmailListener {

    private final AuthEmailSender sender;
    private final Executor executor;
    private final Clock clock;

    public AuthEmailListener(AuthEmailSender sender, @Qualifier("authMailExecutor") Executor executor, Clock clock) {
        this.sender = sender;
        this.executor = executor;
        this.clock = clock;
    }

    @TransactionalEventListener
    public void afterCommit(AuthEmail email) {
        try {
            executor.execute(() -> {
                if (!email.expiresAt().isAfter(clock.instant())) return;
                try {
                    sender.send(email);
                } catch (RuntimeException failure) {
                    // SMTP exceptions may contain recipients/URLs/credentials; do not log them.
                    log.warn(
                        "Auth email delivery failed; purpose={}, cause={}",
                        email.purpose(),
                        failure.getClass().getSimpleName()
                    );
                }
            });
        } catch (RejectedExecutionException failure) {
            log.warn("Auth email queue is full; purpose={}", email.purpose());
        }
        // TODO: Nên thêm transactional outbox với payload được mã hóa và worker retry để bảo đảm
        // email không mất khi restart/SMTP lỗi. MVP dùng gửi sau commit và cho phép yêu cầu lại link.
    }
}
