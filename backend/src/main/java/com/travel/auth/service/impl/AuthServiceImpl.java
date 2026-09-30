package com.travel.auth.service.impl;

import com.travel.auth.dto.request.LoginRequest;
import com.travel.auth.dto.request.RefreshTokenRequest;
import com.travel.auth.dto.request.RegisterRequest;
import com.travel.auth.dto.response.AuthResponse;
import com.travel.auth.dto.response.RegistrationResponse;
import com.travel.auth.entity.RefreshToken;
import com.travel.auth.repository.RefreshTokenRepository;
import com.travel.auth.service.AuthService;
import com.travel.auth.service.EmailActionService;
import com.travel.auth.config.AuthEmailProperties;
import com.travel.auth.token.ActionTokenCodec;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // BCrypt vẫn chạy khi email không tồn tại để hạn chế phân biệt tài khoản qua thời gian phản hồi.
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$71iAveyKYFu0t2qhxc91UuFh1HfNYqzmzAwyfWDtxN3Pv1ZuDv7Tm";
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailActionService emailActions;
    private final AuthEmailProperties emailProperties;

    @Override
    @Transactional
    public RegistrationResponse register(RegisterRequest request, String locale) {
        emailActions.ensureConfigured();
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // Chỉ chuyển lỗi unique email thành 409; lỗi DB khác vẫn là lỗi nội bộ.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                        && "uk_users_email".equals(violation.getConstraintName())) {
                    throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
                }
            }
            throw exception;
        }
        emailActions.sendInitialVerification(user, locale);
        return new RegistrationResponse(true, emailProperties.resendCooldown().toSeconds());
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.lockByEmail(normalizeEmail(request.getEmail())).orElse(null);
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(),
                user != null ? user.getPasswordHash() : DUMMY_PASSWORD_HASH);
        if (user == null || !passwordMatches) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        requireActiveUser(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        final Claims claims;
        final UUID userId;
        try {
            claims = jwtUtils.extractAllClaims(request.getRefreshToken(), TokenType.REFRESH_TOKEN);
            String claimedUserId = claims.get("userId", String.class);
            if (claimedUserId == null) {
                throw new IllegalArgumentException("Missing user ID");
            }
            userId = UUID.fromString(claimedUserId);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        User user = userRepository.lockById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
        requireActiveUser(user);
        Long version = claims.get("sessionVersion", Long.class);
        if (!user.getEmail().equals(claims.getSubject()) || user.getSessionVersion() != (version == null ? 0 : version)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (refreshTokenRepository.consume(hashToken(request.getRefreshToken()), userId, Instant.now()) != 1) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.revoke(hashToken(request.getRefreshToken()));
        // TODO: Nên thêm thu hồi access token theo phiên nếu cần đăng xuất có hiệu lực ngay trên server;
        // hiện token còn hiệu lực đến khi hết hạn. Có thể dùng session version hoặc Redis khi chạy nhiều instance.
    }

    private AuthResponse issueTokens(User user) {
        Set<String> permissions = user.getRole() != null
                ? rolePermissionRepository.findPermissionNamesByRole(user.getRole()) : Set.of();
        CustomUserDetails details = new CustomUserDetails(user, permissions);
        String accessToken = jwtUtils.generateAccessToken(details);
        String refreshToken = jwtUtils.generateRefreshToken(details);
        refreshTokenRepository.save(new RefreshToken(user.getId(), hashToken(refreshToken),
                Instant.now().plusMillis(jwtUtils.getJwtRefreshExpiration())));
        // TODO: Nên thêm tác vụ xóa refresh token hết hạn định kỳ khi số lượng phiên tăng.
        return AuthResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
                .expiresIn(jwtUtils.getJwtExpiration())
                .user(UserSummaryResponse.builder().id(user.getId()).email(user.getEmail())
                        .fullName(user.getFullName()).role(user.getRole()).avatarUrl(user.getAvatarUrl()).build())
                .build();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireActiveUser(User user) {
        if (user.isDeleted()) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
        }
        if (!user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    public static String hashToken(String token) {
        return ActionTokenCodec.hash(token);
    }
}
