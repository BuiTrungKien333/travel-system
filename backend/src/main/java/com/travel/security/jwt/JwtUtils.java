package com.travel.security.jwt;

import com.travel.security.model.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtils {

    @Value("${app.jwt.secret-key}")
    private String secretKey;

    @Value("${app.jwt.refresh-key}")
    private String refreshKey;

    @Value("${app.jwt.access.expiration}")
    private long jwtExpiration;

    @Value("${app.jwt.refresh.expiration}")
    private long jwtRefreshExpiration;

    public long getJwtExpiration() {
        return jwtExpiration;
    }

    public long getJwtRefreshExpiration() { return jwtRefreshExpiration; }

    private SecretKey accessSigningKey;
    private SecretKey refreshSigningKey;

    @PostConstruct
    private void initKeys() {
        this.accessSigningKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.refreshSigningKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshKey));
    }

    // --- GENERATE TOKEN ---
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        if (userDetails instanceof CustomUserDetails user) {
            extraClaims.put("userId", user.getId());
            extraClaims.put("sessionVersion", user.getSessionVersion());
            extraClaims.put("authorities", user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList());
        }

        return buildToken(extraClaims, userDetails, TokenType.ACCESS_TOKEN, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (userDetails instanceof CustomUserDetails user) {
            claims.put("userId", user.getId());
            claims.put("sessionVersion", user.getSessionVersion());
        }
        return buildToken(claims, userDetails, TokenType.REFRESH_TOKEN, jwtRefreshExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, TokenType type, long expiration) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extraClaims)
                .claim("tokenType", type.name())
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(getSignInKey(type), Jwts.SIG.HS256)
                .compact();
    }


    // --- VALIDATE TOKEN ---
    public boolean isTokenValid(String token, UserDetails userDetails, TokenType type) {
        try {
            final String username = extractUsername(token, type);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token, type);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token type={}", type);
            return false;
        }
    }

    private boolean isTokenExpired(String token, TokenType type) {
        return extractClaim(token, type, Claims::getExpiration).before(new Date());
    }

    // --- EXTRACT DATA ---
    public String extractUsername(String token, TokenType type) {
        return extractClaim(token, type, Claims::getSubject);
    }

    public <T> T extractClaim(String token, TokenType type, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token, type);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token, TokenType type) {
        return Jwts.parser()
                .verifyWith(getSignInKey(type))
                .require("tokenType", type.name())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // --- GET KEY HELPER ---
    private SecretKey getSignInKey(TokenType type) {
        return type == TokenType.REFRESH_TOKEN ? refreshSigningKey : accessSigningKey;
    }
}
