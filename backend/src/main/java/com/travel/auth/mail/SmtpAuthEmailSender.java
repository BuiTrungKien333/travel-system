package com.travel.auth.mail;

import com.travel.auth.config.AuthEmailProperties;
import com.travel.auth.entity.ActionPurpose;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

@Component
@RequiredArgsConstructor
public class SmtpAuthEmailSender implements AuthEmailSender {

    private final JavaMailSender sender;
    private final AuthEmailProperties properties;

    @Override
    public void send(AuthEmail email) {
        boolean verify = email.purpose() == ActionPurpose.VERIFY_EMAIL;
        boolean google = email.purpose() == ActionPurpose.GOOGLE_LINK;
        boolean vietnamese = "vi".equals(email.locale());

        // Fragment stays on the client: it is not sent to web servers in a GET request.
        String link = (google ? email.linkOverride() : verify ? properties.verifyEmailUrl() : properties.resetPasswordUrl()) + "#token=" + email.token();
        String subject = vietnamese ? (verify ? "Tripzy — Xác thực email" : "Tripzy — Đặt lại mật khẩu")
                : (verify ? "Tripzy — Verify your email" : "Tripzy — Reset your password");
        String action = vietnamese ? (google ? "Xác nhận đăng nhập Google" : verify ? "Xác thực email" : "Đặt lại mật khẩu")
                : (google ? "Confirm Google sign-in" : verify ? "Verify email" : "Reset password");

        String intro = vietnamese ? (verify ? "Mở liên kết và chọn Xác thực email để hoàn tất đăng ký."
                : "Mở liên kết để tạo mật khẩu mới. Sau khi đổi mật khẩu, mọi phiên cũ sẽ bị thu hồi.")
                : (verify ? "Open the link and choose Verify email to complete registration."
                : "Open the link to choose a new password. All previous sessions will be revoked.");

        long minutes = (verify ? properties.verificationTtl() : properties.resetTtl()).toMinutes();
        if (google) {
            subject = vietnamese ? "Tripzy — Xác nhận đăng nhập Google" : "Tripzy — Confirm Google sign-in";
            intro = vietnamese ? "Bạn vừa yêu cầu đăng nhập bằng Google. Mở liên kết và xác nhận để liên kết tài khoản Google và đăng nhập. Chỉ xác nhận nếu bạn vừa thực hiện yêu cầu này."
                    : "You requested Google sign-in. Open the link and confirm to link your Google account and sign in. Confirm only if you just made this request.";
            minutes = Math.max(1, java.time.Duration.between(java.time.Instant.now(), email.expiresAt()).toMinutes());
        }

        String details = vietnamese ? "Liên kết dùng một lần, hết hạn sau " + minutes + " phút. Nếu bạn không yêu cầu, hãy bỏ qua email này."
                : "This single-use link expires after " + minutes + " minutes. If you did not request it, ignore this email.";
        try {
            var message = sender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setValidateAddresses(true);
            helper.setFrom(properties.from(), "Tripzy");
            helper.setTo(email.recipient());
            helper.setSubject(subject);
            String plainText = intro + "\n\n" + link + "\n\n" + details;
            String html = "<!doctype html><html><body><p>" + escapeHtml(intro) + "</p><p><a href=\""
                    + escapeHtml(link) + "\" style=\"display:inline-block;padding:12px 20px;border-radius:6px;"
                    + "background:#168c97;color:#ffffff;text-decoration:none;font-weight:600\">"
                    + escapeHtml(action) + "</a></p><p>" + escapeHtml(details)
                    + "</p><p><small>" + escapeHtml(link) + "</small></p></body></html>";
            helper.setText(plainText, html);
            sender.send(message);
        } catch (MessagingException | UnsupportedEncodingException exception) {
            throw new MailPreparationException("Cannot prepare auth email");
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
