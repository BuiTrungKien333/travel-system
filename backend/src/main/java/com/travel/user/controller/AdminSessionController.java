package com.travel.user.controller;

import com.travel.common.response.ApiResponse;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.util.SecurityUtils;
import com.travel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/session")
@RequiredArgsConstructor
public class AdminSessionController {
    private final UserService users;
    public record Session(UUID id, String fullName, String email, String role, Set<String> permissions) { }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Session> current() {
        var principal = SecurityUtils.getCurrentUserDetails().orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
        var profile = users.getCurrentProfile();
        return ApiResponse.success(new Session(profile.getId(), profile.getFullName(), profile.getEmail(),
                profile.getRole().name(), principal.getPermissions()));
    }
}
