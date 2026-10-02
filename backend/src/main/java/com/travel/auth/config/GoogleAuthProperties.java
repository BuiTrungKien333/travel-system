package com.travel.auth.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.net.URI;

@ConfigurationProperties("app.google")
public record GoogleAuthProperties(boolean enabled, String webClientId, Duration challengeTtl, Duration linkTtl, URI linkUrl, boolean allowInsecureLinks) {
    public GoogleAuthProperties {
        if (enabled && (webClientId == null || !webClientId.matches("[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com")))
            throw new IllegalArgumentException("Configure GOOGLE_WEB_CLIENT_ID before enabling Google login");
        if (challengeTtl == null || challengeTtl.toSeconds() < 1 || challengeTtl.compareTo(Duration.ofMinutes(10)) > 0
                || linkTtl == null || linkTtl.toSeconds() < 1 || linkTtl.compareTo(Duration.ofMinutes(10)) > 0)
            throw new IllegalArgumentException("Google challenge/link TTL must be between one second and ten minutes");
        AuthEmailProperties.checkLink(linkUrl, allowInsecureLinks);
    }
}
