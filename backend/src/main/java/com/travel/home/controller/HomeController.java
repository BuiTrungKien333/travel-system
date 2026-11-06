package com.travel.home.controller;

import com.travel.common.response.ApiResponse;
import com.travel.home.dto.HomeDashboard;
import com.travel.home.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {
    private final HomeService service;
    @GetMapping("/dashboard")
    public ApiResponse<HomeDashboard> dashboard() { return ApiResponse.success(service.get()); }
}
