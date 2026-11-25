package com.travel.planning.ai.intent.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.ai.intent.dto.response.IntentAnalysisResponse;
import com.travel.planning.ai.intent.dto.response.IntentSearchResponse;
import com.travel.planning.ai.intent.service.TravelIntentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({ "dev", "rag-demo" })
@RequestMapping("/api/v1/public/planning/retrieval")
public class TravelIntentController {

    private final TravelIntentService service;

    public TravelIntentController(TravelIntentService service) {
        this.service = service;
    }

    @PostMapping("/intent")
    @Operation(summary = "Extract and validate travel preferences from Vietnamese text; no itinerary is written")
    public ApiResponse<IntentAnalysisResponse> intent(@Valid @RequestBody IntentRequest request) {
        return ApiResponse.success(service.analyze(request.text()));
    }

    @PostMapping("/search-from-text")
    @Operation(summary = "Understand travel text and retrieve curated candidates, without creating a schedule")
    public ApiResponse<IntentSearchResponse> search(@Valid @RequestBody TextSearchRequest request) {
        return ApiResponse.success(service.search(request.text(), request.limit()));
    }

    public record IntentRequest(@NotBlank @Size(max = 1000) String text) {}

    public record TextSearchRequest(@NotBlank @Size(max = 1000) String text, @Min(1) @Max(8) Integer limit) {
        public TextSearchRequest {
            if (limit == null) {
                limit = 4;
            }
        }
    }
}
