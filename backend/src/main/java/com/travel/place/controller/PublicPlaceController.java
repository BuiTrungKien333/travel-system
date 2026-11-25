package com.travel.place.controller;

import com.travel.common.response.ApiResponse;
import com.travel.place.dto.PlaceDiscovery;
import com.travel.place.service.PlaceDiscoveryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/places")
@RequiredArgsConstructor
public class PublicPlaceController {

    private final PlaceDiscoveryService service;

    @GetMapping
    public ApiResponse<PlaceDiscovery.Page> list(
        @RequestParam UUID provinceId,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "vi") String locale
    ) {
        return ApiResponse.success(service.suggestions(null, null, provinceId, q, page, size, locale));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlaceDiscovery.Detail> detail(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "vi") String locale
    ) {
        return ApiResponse.success(service.publicDetail(id, locale));
    }
}
