package com.travel.security.jwt;

import com.travel.security.model.CustomUserDetails;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    // Chuẩn Base64 cho khóa bí mật tối thiểu 256 bits (32 bytes)
    private static final String BASE64_SECRET = Base64.getEncoder().encodeToString(
            "my_super_secret_key_for_access_token_123456".getBytes()
    );
    private static final String BASE64_REFRESH_SECRET = Base64.getEncoder().encodeToString(
            "my_super_secret_key_for_refresh_token_12345".getBytes()
    );

    private CustomUserDetails userDetails;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "refreshKey", BASE64_REFRESH_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpiration", 1800000L); // 30 mins
        ReflectionTestUtils.setField(jwtUtils, "jwtRefreshExpiration", 604800000L); // 7 days
        ReflectionTestUtils.invokeMethod(jwtUtils, "initKeys");

        userId = UUID.randomUUID();
        User user = User.builder()
                .fullName("Nguyen Van Test")
                .email("test@travel.com")
                .passwordHash("hashed_pwd")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        userDetails = new CustomUserDetails(user, Set.of("trip:read", "trip:create"));
    }

    @Test
    @DisplayName("generateAccessToken should create a valid token with user claims and authorities")
    void generateAccessToken_success() {
        String token = jwtUtils.generateAccessToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtils.isTokenValid(token, userDetails, TokenType.ACCESS_TOKEN)).isTrue();
        assertThat(jwtUtils.extractUsername(token, TokenType.ACCESS_TOKEN)).isEqualTo("test@travel.com");

        Claims claims = jwtUtils.extractAllClaims(token, TokenType.ACCESS_TOKEN);
        assertThat(claims.get("userId", String.class)).isEqualTo(userId.toString());

        @SuppressWarnings("unchecked")
        List<String> authorities = claims.get("authorities", List.class);
        assertThat(authorities).contains("ROLE_USER", "trip:read", "trip:create");
    }

    @Test
    @DisplayName("generateRefreshToken should create a valid refresh token")
    void generateRefreshToken_success() {
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtUtils.isTokenValid(refreshToken, userDetails, TokenType.REFRESH_TOKEN)).isTrue();
        assertThat(jwtUtils.extractUsername(refreshToken, TokenType.REFRESH_TOKEN)).isEqualTo("test@travel.com");
    }

    @Test
    @DisplayName("isTokenValid should return false when username does not match")
    void isTokenValid_wrongUser_returnsFalse() {
        String token = jwtUtils.generateAccessToken(userDetails);

        User anotherUser = User.builder()
                .fullName("Another User")
                .email("another@travel.com")
                .passwordHash("pwd")
                .role(Role.USER)
                .build();
        CustomUserDetails anotherDetails = new CustomUserDetails(anotherUser);

        assertThat(jwtUtils.isTokenValid(token, anotherDetails, TokenType.ACCESS_TOKEN)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid should return false when access token is validated against refresh key")
    void isTokenValid_wrongTokenType_returnsFalse() {
        String accessToken = jwtUtils.generateAccessToken(userDetails);

        // Validating ACCESS_TOKEN with REFRESH_TOKEN key should fail signature check
        assertThat(jwtUtils.isTokenValid(accessToken, userDetails, TokenType.REFRESH_TOKEN)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid should return false when token is expired")
    void isTokenValid_expiredToken_returnsFalse() {
        // Set expiration to negative value (already expired)
        ReflectionTestUtils.setField(jwtUtils, "jwtExpiration", -1000L);
        String expiredToken = jwtUtils.generateAccessToken(userDetails);

        // Reset expiration
        ReflectionTestUtils.setField(jwtUtils, "jwtExpiration", 1800000L);

        assertThat(jwtUtils.isTokenValid(expiredToken, userDetails, TokenType.ACCESS_TOKEN)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid should return false for malformed or null token")
    void isTokenValid_malformedToken_returnsFalse() {
        assertThat(jwtUtils.isTokenValid("invalid.token.structure", userDetails, TokenType.ACCESS_TOKEN)).isFalse();
        assertThat(jwtUtils.isTokenValid("", userDetails, TokenType.ACCESS_TOKEN)).isFalse();
        assertThat(jwtUtils.isTokenValid(null, userDetails, TokenType.ACCESS_TOKEN)).isFalse();
    }
}
