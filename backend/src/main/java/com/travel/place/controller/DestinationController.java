package com.travel.place.controller;

import com.travel.common.response.ApiResponse;
import com.travel.place.dto.DestinationPage;
import com.travel.place.service.DestinationCatalogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/destinations")
@RequiredArgsConstructor
@Tag(name = "Destinations")
@SecurityRequirement(name = "bearerAuth")
public class DestinationController {

    private final DestinationCatalogService service;

    @GetMapping
    public ApiResponse<DestinationPage> list(
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.list(q, page, size));
    }
}
