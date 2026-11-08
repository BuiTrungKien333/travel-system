package com.travel.auth.mail;

import com.travel.auth.config.AuthEmailProperties;
import com.travel.auth.entity.ActionPurpose;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import java.net.URI;
import java.time.*;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpAuthEmailSenderTest {
    private final AuthEmailProperties properties = new AuthEmailProperties(true, "no-reply@example.com",
            URI.create("https://app.example.com/verify-email"), URI.create("https://app.example.com/reset-password"),
            Duration.ofHours(24), Duration.ofMinutes(30), Duration.ofSeconds(60), false);

    @Test void verificationMessageIsUtf8PlainTextAndTokenIsInFragment() throws Exception {
        JavaMailSender transport = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(transport.createMimeMessage()).thenReturn(message);
        new SmtpAuthEmailSender(transport, properties).send(new AuthEmail("user@example.com", "a".repeat(43),
                ActionPurpose.VERIFY_EMAIL, "vi", Instant.now().plusSeconds(60)));
        verify(transport).send(message);
        message.saveChanges();
        assertThat(message.getSubject()).contains("Xác thực email");
        assertThat(message.getContentType()).containsIgnoringCase("text/plain").containsIgnoringCase("utf-8");
        assertThat(message.getContent().toString()).contains("https://app.example.com/verify-email#token=" + "a".repeat(43)).contains("1440 phút");
        assertThat(message.getContent().toString()).doesNotContain("?token=");
    }
    @Test void resetMessageUsesConfiguredResetDestinationAndEnglishCopy() throws Exception {
        JavaMailSender transport = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(transport.createMimeMessage()).thenReturn(message);
        new SmtpAuthEmailSender(transport, properties).send(new AuthEmail("user@example.com", "a".repeat(43),
                ActionPurpose.PASSWORD_RESET, "en", Instant.now().plusSeconds(60)));
        assertThat(message.getSubject()).contains("Reset your password");
        assertThat(message.getContent().toString()).contains("/reset-password#token=").contains("30 minutes").contains("sessions will be revoked");
    }
    @Test void deliveryFailureCannotBreakCommittedRegistrationOrExposeRawException() {
        AuthEmailSender sender = mock(AuthEmailSender.class);
        AuthEmail email = new AuthEmail("private@example.com", "a".repeat(43), ActionPurpose.VERIFY_EMAIL, "vi", Instant.now().plusSeconds(60));
        doThrow(new MailSendException("sensitive provider response")).when(sender).send(email);
        assertThatCode(() -> new AuthEmailListener(sender, Runnable::run, Clock.systemUTC()).afterCommit(email)).doesNotThrowAnyException();
        verify(sender).send(email);
    }
    @Test void saturatedQueueCannotBreakCommittedRegistration() {
        AuthEmailSender sender = mock(AuthEmailSender.class);
        var listener = new AuthEmailListener(sender, task -> { throw new RejectedExecutionException(); }, Clock.systemUTC());
        assertThatCode(() -> listener.afterCommit(new AuthEmail("user@example.com", "a".repeat(43),
                ActionPurpose.VERIFY_EMAIL, "vi", Instant.now().plusSeconds(60)))).doesNotThrowAnyException();
        verifyNoInteractions(sender);
    }
}
