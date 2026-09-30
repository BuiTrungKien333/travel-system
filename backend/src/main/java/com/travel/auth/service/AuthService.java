package com.travel.auth.service;

import com.travel.auth.dto.request.LoginRequest;
import com.travel.auth.dto.request.RefreshTokenRequest;
import com.travel.auth.dto.request.RegisterRequest;
import com.travel.auth.dto.response.AuthResponse;

public interface AuthService {

    /**
     * Registers a new user account.
     *
     * @param request registration information (fullName, email, password)
     * @return AuthResponse containing access/refresh tokens and user summary
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Logs in a user using email and password.
     *
     * @param request login information (email, password)
     * @return AuthResponse containing access/refresh tokens and user summary
     */
    AuthResponse login(LoginRequest request);

    /**
     * Issues a new access token using a valid refresh token.
     *
     * @param request refresh token information
     * @return AuthResponse containing access/refresh tokens and user summary
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /** Thu hồi refresh token của thiết bị hiện tại; gọi lặp lại vẫn thành công. */
    void logout(RefreshTokenRequest request);
}
