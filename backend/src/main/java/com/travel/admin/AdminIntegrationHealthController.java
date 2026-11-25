package com.travel.admin;

import com.travel.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!test")
@RequestMapping("/api/v1/admin/integrations/health")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:system:read')")
public class AdminIntegrationHealthController {

    private final AdminIntegrationHealthService service;

    @GetMapping
    public ApiResponse<AdminIntegrationHealthService.Snapshot> snapshot() {
        return ApiResponse.success(service.snapshot());
    }

    @PostMapping("/check")
    public ApiResponse<AdminIntegrationHealthService.Trigger> triggerCheck() {
        return ApiResponse.success(service.triggerChecks());
    }
}
