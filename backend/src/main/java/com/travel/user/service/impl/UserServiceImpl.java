package com.travel.user.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.util.SecurityUtils;
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

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("[USER] Profile fetch failed: User not found in DB - userId={}", currentUserId);
                    return new AppException(ErrorCode.USER_NOT_FOUND);
                });

        if (user.isDeleted()) {
            log.warn("[USER] Profile fetch failed: Account deactivated/deleted - userId={}", currentUserId);
            throw new AppException(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);
        }

        log.info("[USER] Profile retrieved successfully: userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

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
