package com.travel.admin;

import com.travel.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminOverviewController {

    private final AdminOverviewService service;

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewService.Overview> overview(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(service.overview(days));
    }

    @GetMapping("/knowledge")
    public ApiResponse<AdminOverviewService.KnowledgePage> knowledge(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String provinceCode,
        @RequestParam(required = false) String indexStatus
    ) {
        return ApiResponse.success(service.list(page, size, provinceCode, indexStatus));
    }
}
