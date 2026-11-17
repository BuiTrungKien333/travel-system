package com.travel.planning.retrieval.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.retrieval.dto.response.PlaceIndexResponse;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({"dev", "rag-demo", "planning-ai"})
@RequestMapping("/api/v1/admin/planning/retrieval")
public class PlaceAdminIndexController {
    private final PlaceRetrievalService service;
    public PlaceAdminIndexController(PlaceRetrievalService service) { this.service = service; }

    @PostMapping("/index")
    @Operation(summary = "Index approved places for RAG; administrator only",
            description = "Skips current embeddings. Provider failures are reported as failed counts; never updates unapproved places.")
    public ApiResponse<PlaceIndexResponse> index() { return ApiResponse.success(service.index()); }
}
