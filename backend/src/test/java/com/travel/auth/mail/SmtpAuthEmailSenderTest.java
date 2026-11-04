package com.travel.auth.mail;

import com.travel.auth.config.AuthEmailProperties;
import com.travel.auth.entity.ActionPurpose;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
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

    @Test void verificationMessageHasClickableHtmlAndPlainTextFallback() throws Exception {
        JavaMailSender transport = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(transport.createMimeMessage()).thenReturn(message);
        new SmtpAuthEmailSender(transport, properties).send(new AuthEmail("user@example.com", "a".repeat(43),
                ActionPurpose.VERIFY_EMAIL, "vi", Instant.now().plusSeconds(60)));
        verify(transport).send(message);
        message.saveChanges();
        assertThat(message.getSubject()).contains("Xác thực email");
        assertThat(message.getContentType()).containsIgnoringCase("multipart");
        String plain = findBody(message.getContent(), "text/plain");
        String html = findBody(message.getContent(), "text/html");
        assertThat(plain).contains("https://app.example.com/verify-email#token=" + "a".repeat(43)).contains("1440 phút");
        assertThat(plain).doesNotContain("?token=");
        assertThat(html)
                .contains("href=\"https://app.example.com/verify-email#token=" + "a".repeat(43) + "\"")
                .contains(">Xác thực email</a>");
    }
    @Test void resetMessageUsesConfiguredResetDestinationAndEnglishCopy() throws Exception {
        JavaMailSender transport = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(transport.createMimeMessage()).thenReturn(message);
        new SmtpAuthEmailSender(transport, properties).send(new AuthEmail("user@example.com", "a".repeat(43),
                ActionPurpose.PASSWORD_RESET, "en", Instant.now().plusSeconds(60)));
        message.saveChanges();
        assertThat(message.getSubject()).contains("Reset your password");
        String plain = findBody(message.getContent(), "text/plain");
        String html = findBody(message.getContent(), "text/html");
        assertThat(plain).contains("/reset-password#token=").contains("30 minutes").contains("sessions will be revoked");
        assertThat(html).contains("href=\"https://app.example.com/reset-password#token=")
                .contains(">Reset password</a>");
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

    private static String findBody(Object content, String mimeType) throws Exception {
        if (!(content instanceof MimeMultipart multipart)) return null;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object body = part.getContent();
            String nested = findBody(body, mimeType);
            if (nested != null) return nested;
            if (!(body instanceof Multipart) && part.isMimeType(mimeType)) return body.toString();
        }
        return null;
    }
}
