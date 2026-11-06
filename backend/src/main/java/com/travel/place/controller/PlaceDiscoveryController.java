package com.travel.place.controller;

import com.travel.common.response.ApiResponse;
import com.travel.place.dto.PlaceDiscovery;
import com.travel.place.service.PlaceDiscoveryService;
import com.travel.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PlaceDiscoveryController {
    private final PlaceDiscoveryService service;
    @GetMapping("/suggestions")
    public ApiResponse<PlaceDiscovery.Page> suggestions(@RequestParam(required = false) UUID tripId,
            @RequestParam(required = false) UUID provinceId, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "vi") String locale) {
        return ApiResponse.success(service.suggestions(SecurityUtils.getCurrentUserId(), tripId, provinceId, q, page, size, locale));
    }
    @GetMapping("/{placeId}")
    public ApiResponse<PlaceDiscovery.Detail> detail(@PathVariable UUID placeId, @RequestParam(defaultValue = "vi") String locale) {
        return ApiResponse.success(service.detail(SecurityUtils.getCurrentUserId(), placeId, locale));
    }
}
