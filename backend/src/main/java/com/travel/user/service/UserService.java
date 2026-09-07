package com.travel.user.service;

import com.travel.user.dto.request.UpdateProfileRequest;
import com.travel.user.dto.response.UserProfileResponse;

public interface UserService {

    /**
     * Lấy thông tin profile của người dùng đang đăng nhập
     *
     * @return {@link UserProfileResponse} thông tin chi tiết của người dùng
     */
    UserProfileResponse getCurrentProfile();

    /**
     * Cập nhật thông tin profile của người dùng đang đăng nhập
     *
     * @param request DTO chứa thông tin cập nhật (fullName, avatarUrl)
     * @return {@link UserProfileResponse} thông tin người dùng sau khi cập nhật
     */
    UserProfileResponse updateProfile(UpdateProfileRequest request);
}
