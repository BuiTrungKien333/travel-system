package com.travel.planning.retrieval.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;
import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({ "dev", "rag-demo" })
@RequestMapping("/api/v1/public/planning/retrieval")
public class PlaceRetrievalController {

    private final PlaceRetrievalService service;

    public PlaceRetrievalController(PlaceRetrievalService service) {
        this.service = service;
    }

    @GetMapping("/catalog")
    public ApiResponse<List<PlaceCandidate>> catalog() {
        return ApiResponse.success(service.catalog());
    }

    @PostMapping("/search")
    @Operation(
        summary = "Retrieve eligible place candidates for a province; not a complete itinerary or intent parser",
        description = "positiveQuery contains positive interests. Supply exclusions separately as canonical tags and optionally filter to a province. Public in the dev and rag-demo profiles."
    )
    public ApiResponse<PlaceSearchResponse> search(@Valid @RequestBody SearchRequest request) {
        return ApiResponse.success(
            service.search(request.positiveQuery(), request.excludedTags(), request.provinceCode(), request.limit())
        );
    }

    public record SearchRequest(
        @NotBlank @Size(max = 1000) String positiveQuery,
        @Size(max = 10) List<@Pattern(regexp = "[A-Z_]{2,30}") String> excludedTags,
        @Pattern(regexp = "\\d{2}") String provinceCode,
        @Min(1) @Max(8) int limit
    ) {
        public SearchRequest {
            excludedTags = excludedTags == null ? List.of() : List.copyOf(excludedTags);
        }
    }
}
