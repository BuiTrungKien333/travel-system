package com.travel.planning.ai.intent;

import com.travel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("rag-demo")
@RequestMapping("/api/v1/public/planning/retrieval")
public class TravelIntentController {
    private final TravelIntentService service;
    public TravelIntentController(TravelIntentService service) { this.service = service; }

    @PostMapping("/intent")
    @Operation(summary = "Extract and validate travel preferences from Vietnamese text; no itinerary is written")
    public ApiResponse<TravelIntentService.IntentAnalysis> intent(@Valid @RequestBody IntentRequest request) {
        return ApiResponse.success(service.analyze(request.text()));
    }

    @PostMapping("/search-from-text")
    @Operation(summary = "Understand travel text and retrieve curated candidates, without creating a schedule")
    public ApiResponse<TravelIntentService.TextSearchResult> search(@Valid @RequestBody TextSearchRequest request) {
        return ApiResponse.success(service.search(request.text(), request.limit()));
    }

    public record IntentRequest(@NotBlank @Size(max=1000) String text) {}
    public record TextSearchRequest(@NotBlank @Size(max=1000) String text, @Min(1) @Max(8) Integer limit) {
        public TextSearchRequest { if (limit == null) limit = 4; }
    }
}
