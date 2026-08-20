package com.travel.user.service;

import com.travel.user.dto.request.LoginRequest;
import com.travel.user.dto.request.RefreshTokenRequest;
import com.travel.user.dto.request.RegisterRequest;
import com.travel.user.dto.response.AuthResponse;

public interface AuthService {

    /**
     * Đăng ký tài khoản người dùng mới
     *
     * @param request thông tin đăng ký (fullName, email, password)
     * @return AuthResponse chứa access/refresh token và user summary
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Đăng nhập người dùng bằng email và mật khẩu
     *
     * @param request thông tin đăng nhập (email, password)
     * @return AuthResponse chứa access/refresh token và user summary
     */
    AuthResponse login(LoginRequest request);

    /**
     * Cấp phát Access Token mới từ Refresh Token hợp lệ
     *
     * @param request thông tin refresh token
     * @return AuthResponse chứa access/refresh token và user summary
     */
    AuthResponse refreshToken(RefreshTokenRequest request);
}
