package com.travel.planning.retrieval.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.retrieval.dto.response.PlaceIndexResponse;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
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
    public ApiResponse<PlaceIndexResponse> index() {
        return ApiResponse.success(service.index());
    }
}
