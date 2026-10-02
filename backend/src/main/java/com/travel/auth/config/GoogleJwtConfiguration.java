package com.travel.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GoogleAuthProperties.class)
public class GoogleJwtConfiguration {
    @Bean("googleJwtDecoder")
    JwtDecoder googleJwtDecoder(GoogleAuthProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(5000);
        var decoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .restOperations(new RestTemplate(factory)).build();
        decoder.setJwtValidator(validators(properties));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> validators(GoogleAuthProperties properties) {
        OAuth2TokenValidator<Jwt> issuerAudience = jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            if (("accounts.google.com".equals(issuer) || "https://accounts.google.com".equals(issuer))
                    && jwt.getExpiresAt() != null && jwt.getAudience().contains(properties.webClientId())) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        };
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuerAudience);
    }
}
