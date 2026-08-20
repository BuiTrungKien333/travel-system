package com.travel.auth.service.impl;

import com.travel.auth.dto.request.LoginRequest;
import com.travel.auth.dto.request.RefreshTokenRequest;
import com.travel.auth.dto.request.RegisterRequest;
import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.service.AuthService;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.jwt.JwtUtils;
import com.travel.security.jwt.TokenType;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.dto.response.UserSummaryResponse;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.RolePermissionRepository;
import com.travel.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTH_SERVICE")
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String fullName = request.getFullName().trim();
        String email = request.getEmail().trim().toLowerCase();
        log.debug("[AUTH] Processing registration attempt for email: {}", email);

        // 1. Check if email already exists
        if (userRepository.existsByEmail(email)) {
            log.warn("[AUTH] Registration rejected: Email already exists [{}]", email);
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 2. Hash password with BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 3. Create and persist new User entity
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordHash)
                .role(Role.USER)
                .build();
        User savedUser = userRepository.save(user);

        log.info("[AUTH] User registered successfully: email={}, userId={}, role={}", savedUser.getEmail(), savedUser.getId(), savedUser.getRole());

        // 4. Generate tokens & return AuthResponse
        return generateAuthResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.debug("[AUTH] Processing login attempt for email: {}", email);

        // 1. Find user by normalized email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[AUTH] Login rejected: Email not found [{}]", email);
                    return new AppException(ErrorCode.INVALID_CREDENTIALS);
                });

        // 2. Check if account is deactivated or soft-deleted
        if (user.isDeleted()) {
            log.warn("[AUTH] Login rejected: Account deactivated/deleted - userId: {}", user.getId());
            throw new AppException(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
        }

        // 3. Verify password hash using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("[AUTH] Login rejected: Incorrect password - userId: {}", user.getId());
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("[AUTH] Login successful: email={}, userId={}, role={}", user.getEmail(), user.getId(), user.getRole());

        // 4. Generate tokens & return AuthResponse
        return generateAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        final String token = request.getRefreshToken();

        // 1. Extract and validate claims from refresh token
        final Claims claims;
        try {
            claims = jwtUtils.extractAllClaims(token, TokenType.REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[AUTH] Refresh token rejected: Invalid or expired token - reason: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = claims.getSubject();
        log.debug("[AUTH] Processing refresh token request for email: {}", email);

        // 2. Lookup user by subject (email)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[AUTH] Refresh token rejected: User not found with email [{}]", email);
                    return new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
                });

        // 3. Verify user status (not deleted/locked)
        if (user.isDeleted()) {
            log.warn("[AUTH] Refresh token rejected: Account deactivated/deleted - userId: {}", user.getId());
            throw new AppException(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
        }

        log.info("[AUTH] Token refreshed successfully: email={}, userId={}", user.getEmail(), user.getId());

        // 4. Generate new token pair (Token Rotation) & return AuthResponse
        return generateAuthResponse(user);
    }

    /**
     * Tải quyền của người dùng, tạo Claims và đóng gói AuthResponse
     */
    private AuthResponse generateAuthResponse(User user) {
        Set<String> permissions = user.getRole() != null
                ? rolePermissionRepository.findPermissionNamesByRole(user.getRole())
                : Set.of();

        CustomUserDetails userDetails = new CustomUserDetails(user, permissions);
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpiration())
                .user(userSummary)
                .build();
    }
}
