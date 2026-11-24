package com.travel.planning.draft.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.dto.response.DraftResponse;
import com.travel.planning.draft.service.DraftService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({"dev", "rag-demo"})
@RequestMapping("/api/v1/public/planning")
public class DraftController {
    private final DraftService service;

    public DraftController(DraftService service) {
        this.service = service;
    }

    @PostMapping("/generate-draft")
    @Operation(summary = "Preview a 1–3 day itinerary from text or structured preferences, with AI/fallback provenance; no persistence")
    public ApiResponse<DraftResponse> generate(@Valid @RequestBody GenerateDraftRequest request) {
        return ApiResponse.success(service.generate(request));
    }

}
