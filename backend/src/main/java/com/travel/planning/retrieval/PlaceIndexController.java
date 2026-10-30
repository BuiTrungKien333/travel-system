package com.travel.planning.retrieval;

import com.travel.common.response.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("rag-demo")
@RequestMapping("/api/v1/public/planning/retrieval")
public class PlaceIndexController {
    private final PlaceRetrievalService service;

    public PlaceIndexController(PlaceRetrievalService service) {
        this.service = service;
    }

    @PostMapping("/index")
    public ApiResponse<PlaceRetrievalService.IndexResult> index() {
        return ApiResponse.success(service.index());
    }
}
