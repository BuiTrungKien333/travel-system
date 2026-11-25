package com.travel.security.util;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.model.CustomUserDetails;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Lấy CustomUserDetails của người dùng hiện tại từ SecurityContext
     */
    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    /**
     * Lấy UUID của người dùng hiện tại, ném AppException(ErrorCode.UNAUTHORIZED) nếu chưa đăng nhập
     */
    public static UUID getCurrentUserId() {
        return getCurrentUserDetails()
            .map(CustomUserDetails::getId)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * Lấy email của người dùng hiện tại, ném AppException(ErrorCode.UNAUTHORIZED) nếu chưa đăng nhập
     */
    public static String getCurrentUserEmail() {
        return getCurrentUserDetails()
            .map(CustomUserDetails::getEmail)
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }
}
