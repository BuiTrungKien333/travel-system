package com.travel.user.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.util.SecurityUtils;
import com.travel.user.dto.request.UpdateProfileRequest;
import com.travel.user.dto.response.UserProfileResponse;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import com.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER_SERVICE")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("[USER] Fetching profile for current user: userId={}", currentUserId);

        User user = getUserOrThrow(currentUserId, "Profile fetch");

        log.debug("[USER] Profile retrieved: userId={}", user.getId());

        return mapToUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("[USER] Updating profile for current user: userId={}", currentUserId);

        User user = getUserOrThrow(currentUserId, "Profile update");

        user.setFullName(request.getFullName().trim());
        user.setAvatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl().trim() : null);

        User updatedUser = userRepository.save(user);

        log.debug("[USER] Profile updated: userId={}", updatedUser.getId());

        return mapToUserProfileResponse(updatedUser);
    }

    private User getUserOrThrow(UUID userId, String action) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[USER] {} failed: User not found in DB - userId={}", action, userId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        if (user.isDeleted()) {
            log.warn("[USER] {} failed: Account deactivated/deleted - userId={}", action, userId);
            throw new AppException(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
        }

        return user;
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
