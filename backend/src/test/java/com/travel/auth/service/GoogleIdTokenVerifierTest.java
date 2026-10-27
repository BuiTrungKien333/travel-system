package com.travel.auth.service;

import com.travel.auth.config.*;
import com.travel.auth.token.ActionTokenCodec;
import com.travel.common.exception.*;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.net.URI;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.time.*;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;

class GoogleIdTokenVerifierTest {
    private static final String CLIENT = "test-client.apps.googleusercontent.com", NONCE = "a".repeat(64);
    private KeyPair keys;
    private GoogleIdTokenVerifier verifier;
    @BeforeEach void setup() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); keys = generator.generateKeyPair();
        var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
        decoder.setJwtValidator(GoogleJwtConfiguration.validators(new GoogleAuthProperties(true, CLIENT, Duration.ofMinutes(5),
                Duration.ofMinutes(10), URI.create("https://app.example.com/google-link"), false)));
        verifier = new GoogleIdTokenVerifier(decoder);
    }
    private String token(String issuer, String audience, Instant expiry, String nonce, String email, boolean verified, String domain) {
        return Jwts.builder().issuer(issuer).subject("google-subject").audience().add(audience).and()
                .issuedAt(Date.from(Instant.now())).expiration(Date.from(expiry)).claim("nonce", nonce)
                .claim("email", email).claim("email_verified", verified).claim("hd", domain)
                .claim("name", "Google User").signWith(keys.getPrivate(), Jwts.SIG.RS256).compact();
    }
    private String valid(String email, String domain) { return token("https://accounts.google.com", CLIENT, Instant.now().plusSeconds(300), NONCE, email, true, domain); }
    private void invalid(String token) {
        assertThatThrownBy(() -> verifier.verify(token, ActionTokenCodec.hash(NONCE))).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_GOOGLE_TOKEN));
    }
    @Test void acceptsRealRsaSignatureAndNormalizesGmail() {
        var identity = verifier.verify(valid("USER@gmail.com", null), ActionTokenCodec.hash(NONCE));
        assertThat(identity.email()).isEqualTo("user@gmail.com"); assertThat(identity.authoritativeEmail()).isTrue();
    }
    @Test void workspaceRequiresVerifiedEmailAndThirdPartyRequiresEmailProof() {
        assertThat(verifier.verify(valid("user@company.com", "company.com"), ActionTokenCodec.hash(NONCE)).authoritativeEmail()).isTrue();
        assertThat(verifier.verify(valid("user@example.com", null), ActionTokenCodec.hash(NONCE)).authoritativeEmail()).isFalse();
        invalid(token("accounts.google.com", CLIENT, Instant.now().plusSeconds(300), NONCE, "user@gmail.com", false, null));
    }
    @Test void rejectsWrongAudienceIssuerExpiryAndNonce() {
        invalid(token("https://evil.example.com", CLIENT, Instant.now().plusSeconds(300), NONCE, "user@gmail.com", true, null));
        invalid(token("accounts.google.com", "other-client", Instant.now().plusSeconds(300), NONCE, "user@gmail.com", true, null));
        invalid(token("accounts.google.com", CLIENT, Instant.now().minusSeconds(120), NONCE, "user@gmail.com", true, null));
        invalid(token("accounts.google.com", CLIENT, Instant.now().plusSeconds(300), "wrong", "user@gmail.com", true, null));
    }
    @Test void rejectsTamperedJwtAndUnsignedJwt() {
        String signed = valid("user@gmail.com", null);
        String[] parts = signed.split("\\."); invalid(parts[0] + "." + parts[1] + "." + "a".repeat(parts[2].length()));
        invalid(Jwts.builder().subject("google-subject").compact());
    }
}
