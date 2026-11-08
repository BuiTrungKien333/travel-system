package com.travel.user.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.dto.request.UpdateProfileRequest;
import com.travel.user.dto.response.UserProfileResponse;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.UserRepository;
import com.travel.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User mockUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .fullName("Nguyen Van A")
                .email("user@travel.com")
                .passwordHash("hashed_password")
                .role(Role.USER)
                .avatarUrl("https://example.com/avatar.png")
                .build();

        // Giả lập user đã đăng nhập trong SecurityContext
        CustomUserDetails userDetails = new CustomUserDetails(userId, "user@travel.com", Role.USER, List.of());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentProfile should return profile response when user is found and active")
    void getCurrentProfile_success_returnsUserProfileResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserProfileResponse response = userService.getCurrentProfile();

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("user@travel.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getRole()).isEqualTo(Role.USER);
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getCurrentProfile should throw USER_NOT_FOUND when user does not exist in DB")
    void getCurrentProfile_userNotFound_throwsAppException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentProfile())
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getCurrentProfile should throw ACCOUNT_LOCKED_OR_DELETED when user is soft deleted")
    void getCurrentProfile_userDeleted_throwsAppException() {
        mockUser.softDelete();
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> userService.getCurrentProfile())
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);

        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getCurrentProfile should throw UNAUTHORIZED when no user is logged in")
    void getCurrentProfile_unauthenticated_throwsAppException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> userService.getCurrentProfile())
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("updateProfile should update user fields and return updated profile")
    void updateProfile_success_updatesFieldsAndReturnsResponse() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("  Nguyen Van B  ")
                .avatarUrl("  https://example.com/new-avatar.jpg  ")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateProfile(request);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(response.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");
        assertThat(mockUser.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(mockUser.getAvatarUrl()).isEqualTo("https://example.com/new-avatar.jpg");

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("updateProfile with null avatarUrl should set avatarUrl to null")
    void updateProfile_nullAvatar_setsAvatarToNull() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van B")
                .avatarUrl(null)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.updateProfile(request);

        assertThat(response.getAvatarUrl()).isNull();
        assertThat(mockUser.getAvatarUrl()).isNull();

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("updateProfile should throw USER_NOT_FOUND when user does not exist in DB")
    void updateProfile_userNotFound_throwsAppException() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van B")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile should throw ACCOUNT_LOCKED_OR_DELETED when user is soft deleted")
    void updateProfile_userDeleted_throwsAppException() {
        mockUser.softDelete();
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van B")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED_OR_DELETED);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile should throw UNAUTHORIZED when no user is logged in")
    void updateProfile_unauthenticated_throwsAppException() {
        SecurityContextHolder.clearContext();
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyen Van B")
                .build();

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }
}
