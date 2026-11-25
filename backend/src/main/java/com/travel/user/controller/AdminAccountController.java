package com.travel.user.controller;

import com.travel.common.response.ApiResponse;
import com.travel.user.service.AdminAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService service;

    @GetMapping
    public ApiResponse<AdminAccountService.Page> list(
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.list(q, page, size));
    }

    @PatchMapping("/{id}")
    public ApiResponse<Void> change(@PathVariable UUID id, @Valid @RequestBody AdminAccountService.Change request) {
        service.change(id, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/events")
    public ApiResponse<AdminAccountService.Events> events(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.events(page, size));
    }
}
