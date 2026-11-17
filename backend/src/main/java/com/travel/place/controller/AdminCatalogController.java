package com.travel.place.controller;

import com.travel.common.response.ApiResponse;
import com.travel.place.service.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final AdminCatalogService service;
    @GetMapping
    public ApiResponse<AdminCatalogService.Page> list(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return ApiResponse.success(service.list(page,size)); }
    @PatchMapping("/{id}")
    public ApiResponse<Void> review(@PathVariable UUID id, @Valid @RequestBody AdminCatalogService.Review request) {
        service.review(id,request);
        return ApiResponse.success(null);
    }
}
