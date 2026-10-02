package com.travel.auth.service;

import com.travel.auth.token.ActionTokenCodec;
import com.travel.common.exception.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Only trusted decoded Google claims cross the provider boundary. Never accept client profile data. */
@Service
public class GoogleIdTokenVerifier {
    private final JwtDecoder decoder;
    public GoogleIdTokenVerifier(@Qualifier("googleJwtDecoder") JwtDecoder decoder) { this.decoder = decoder; }

    public Identity verify(String rawToken, String expectedNonceHash) {
        final Jwt jwt;
        try { jwt = decoder.decode(rawToken); }
        catch (BadJwtException exception) { throw invalid(); }
        catch (JwtException exception) { throw new AppException(ErrorCode.GOOGLE_SERVICE_UNAVAILABLE); }
        try {
            String nonce = jwt.getClaimAsString("nonce");
            String subject = jwt.getSubject(), email = jwt.getClaimAsString("email");
            if (nonce == null || !MessageDigest.isEqual(ActionTokenCodec.hash(nonce).getBytes(StandardCharsets.US_ASCII),
                    expectedNonceHash.getBytes(StandardCharsets.US_ASCII)) || subject == null || subject.isBlank() || subject.length() > 255
                    || email == null || email.length() > 255 || !email.contains("@") || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")))
                throw invalid();
            email = email.trim().toLowerCase(Locale.ROOT);
            String domain = jwt.getClaimAsString("hd");
            boolean authoritative = email.endsWith("@gmail.com") || (domain != null && !domain.isBlank());
            return new Identity(subject, email, jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"), authoritative);
        } catch (IllegalArgumentException exception) { throw invalid(); }
    }
    private AppException invalid() { return new AppException(ErrorCode.INVALID_GOOGLE_TOKEN); }
    public record Identity(String subject, String email, String name, String picture, boolean authoritativeEmail) {
        @Override public String toString() { return "GoogleIdentity[redacted]"; }
    }
}
