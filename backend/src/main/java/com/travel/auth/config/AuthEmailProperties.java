package com.travel.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("app.auth")
public record AuthEmailProperties(boolean emailEnabled, String from, URI verifyEmailUrl, URI resetPasswordUrl,
                                  Duration verificationTtl, Duration resetTtl, Duration resendCooldown,
                                  boolean allowInsecureLinks) {
    public AuthEmailProperties {
        checkDuration(verificationTtl);
        checkDuration(resetTtl);
        checkDuration(resendCooldown);
        checkLink(verifyEmailUrl, allowInsecureLinks);
        checkLink(resetPasswordUrl, allowInsecureLinks);
        if (emailEnabled && (from == null || from.isBlank() || from.contains("\r") || from.contains("\n"))) {
            throw new IllegalArgumentException("AUTH_EMAIL_FROM must be configured when email is enabled");
        }
        if (emailEnabled && ((verifyEmailUrl.getHost() != null && verifyEmailUrl.getHost().endsWith(".invalid"))
                || (resetPasswordUrl.getHost() != null && resetPasswordUrl.getHost().endsWith(".invalid")))) {
            throw new IllegalArgumentException("Configure real auth link destinations before enabling email");
        }
    }

    private static void checkDuration(Duration value) {
        if (value == null || value.isNegative() || value.isZero() || value.toSeconds() < 1) {
            throw new IllegalArgumentException("Auth email durations must be at least one second");
        }
    }

    public static void checkLink(URI url, boolean allowInsecure) {
        boolean https = url != null && "https".equals(url.getScheme()) && url.getHost() != null;
        boolean dev = url != null && allowInsecure &&
                ((("http".equals(url.getScheme()) || "exp".equals(url.getScheme()) || "exps".equals(url.getScheme())) && url.getHost() != null)
                        || "travelplannermobileui".equals(url.getScheme()));
        if ((!https && !dev) || url.getUserInfo() != null || url.getQuery() != null || url.getFragment() != null) {
            throw new IllegalArgumentException("Auth links require a trusted HTTPS URL without credentials/query/fragment; dev may allow the app scheme");
        }
    }
}
