package com.travel.auth.service;

import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.entity.RefreshToken;
import com.travel.auth.repository.RefreshTokenRepository;
import com.travel.auth.token.ActionTokenCodec;
import com.travel.security.jwt.JwtUtils;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.dto.response.UserSummaryResponse;
import com.travel.user.entity.User;
import com.travel.user.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Set;

/** Called within the password/Google authentication transaction after user status checks. */
@Service
@RequiredArgsConstructor
public class AuthTokenIssuer {
    private final RolePermissionRepository permissions;
    private final JwtUtils jwt;
    private final RefreshTokenRepository tokens;

    public AuthResponse issue(User user) {
        var authorities = user.getRole() == null ? Set.<String>of() : permissions.findPermissionNamesByRole(user.getRole());
        var principal = new CustomUserDetails(user, authorities);
        String access = jwt.generateAccessToken(principal), refresh = jwt.generateRefreshToken(principal);
        tokens.save(new RefreshToken(user.getId(), ActionTokenCodec.hash(refresh), Instant.now().plusMillis(jwt.getJwtRefreshExpiration())));
        return AuthResponse.builder().accessToken(access).refreshToken(refresh).tokenType("Bearer")
                .expiresIn(jwt.getJwtExpiration()).user(UserSummaryResponse.builder()
                        .id(user.getId()).email(user.getEmail()).fullName(user.getFullName()).role(user.getRole())
                        .avatarUrl(user.getAvatarUrl()).build()).build();
    }
}
