package com.travel.user.service;

import com.travel.user.dto.response.UserProfileResponse;

public interface UserService {

    /**
     * Lấy thông tin profile của người dùng đang đăng nhập
     *
     * @return {@link UserProfileResponse} thông tin chi tiết của người dùng
     */
    UserProfileResponse getCurrentProfile();
}
