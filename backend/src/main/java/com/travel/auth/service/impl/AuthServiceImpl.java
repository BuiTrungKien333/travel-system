package com.travel.auth.service.impl;

import com.travel.auth.dto.request.LoginRequest;
import com.travel.auth.dto.request.RefreshTokenRequest;
import com.travel.auth.dto.request.RegisterRequest;
import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.service.AuthService;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.jwt.JwtUtils;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.dto.response.UserSummaryResponse;
import com.travel.user.entity.User;
import com.travel.user.repository.RolePermissionRepository;
import com.travel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        throw new UnsupportedOperationException("Register feature will be implemented in the upcoming step");
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

        // 4. Load permissions for user's role
        Set<String> permissions = user.getRole() != null
                ? rolePermissionRepository.findPermissionNamesByRole(user.getRole())
                : Set.of();

        // 5. Create Principal UserDetails and generate tokens
        CustomUserDetails userDetails = new CustomUserDetails(user, permissions);
        String accessToken = jwtUtils.generateAccessToken(userDetails);
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        log.info("[AUTH] Login successful: email={}, userId={}, role={}", user.getEmail(), user.getId(), user.getRole());

        // 6. Build and return AuthResponse
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

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        throw new UnsupportedOperationException("RefreshToken feature will be implemented in the upcoming step");
    }
}
