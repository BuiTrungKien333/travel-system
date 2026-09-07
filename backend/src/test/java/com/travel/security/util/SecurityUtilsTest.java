package com.travel.security.util;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    private final UUID testUserId = UUID.randomUUID();
    private final String testEmail = "test@travel.com";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUserDetails should return CustomUserDetails when authenticated")
    void getCurrentUserDetails_authenticated_returnsUserDetails() {
        CustomUserDetails userDetails = new CustomUserDetails(testUserId, testEmail, Role.USER, List.of());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<CustomUserDetails> result = SecurityUtils.getCurrentUserDetails();

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(testUserId);
        assertThat(result.get().getEmail()).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("getCurrentUserDetails should return empty Optional when not authenticated")
    void getCurrentUserDetails_unauthenticated_returnsEmpty() {
        Optional<CustomUserDetails> result = SecurityUtils.getCurrentUserDetails();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getCurrentUserId should return UUID when authenticated")
    void getCurrentUserId_authenticated_returnsUserId() {
        CustomUserDetails userDetails = new CustomUserDetails(testUserId, testEmail, Role.USER, List.of());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID userId = SecurityUtils.getCurrentUserId();

        assertThat(userId).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("getCurrentUserId should throw UNAUTHORIZED AppException when not authenticated")
    void getCurrentUserId_unauthenticated_throwsAppException() {
        assertThatThrownBy(SecurityUtils::getCurrentUserId)
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("getCurrentUserEmail should return email when authenticated")
    void getCurrentUserEmail_authenticated_returnsEmail() {
        CustomUserDetails userDetails = new CustomUserDetails(testUserId, testEmail, Role.USER, List.of());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String email = SecurityUtils.getCurrentUserEmail();

        assertThat(email).isEqualTo(testEmail);
    }

    @Test
    @DisplayName("getCurrentUserEmail should throw UNAUTHORIZED AppException when not authenticated")
    void getCurrentUserEmail_unauthenticated_throwsAppException() {
        assertThatThrownBy(SecurityUtils::getCurrentUserEmail)
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
