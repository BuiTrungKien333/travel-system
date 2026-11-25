package com.travel.place.controller;

import com.travel.common.response.ApiResponse;
import com.travel.place.service.MapboxAddressService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class MapboxAddressController {

    private final MapboxAddressService service;

    @GetMapping("/addresses")
    public ApiResponse<MapboxAddressService.Results> search(
        @RequestParam String q,
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false) Double longitude,
        @RequestParam(defaultValue = "vi") String locale,
        HttpServletResponse response
    ) {
        response.setHeader("Cache-Control", "private, no-store");
        return ApiResponse.success(service.search(q, latitude, longitude, locale));
    }
}
