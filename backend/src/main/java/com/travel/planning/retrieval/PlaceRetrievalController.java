package com.travel.planning.retrieval;

import com.travel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Profile("rag-demo")
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
    @Operation(summary = "Retrieve curated Da Nang demo candidates; not a complete itinerary or intent parser",
            description = "positiveQuery contains positive interests. Supply exclusions separately as canonical tags. Public only in the rag-demo profile.")
    public ApiResponse<PlaceRetrievalService.SearchResult> search(@Valid @RequestBody SearchRequest request) {
        return ApiResponse.success(service.search(request.positiveQuery(), request.excludedTags(), request.limit()));
    }

    public record SearchRequest(@NotBlank @Size(max = 1000) String positiveQuery,
                                @Size(max = 10) List<@Pattern(regexp = "[A-Z_]{2,30}") String> excludedTags,
                                @Min(1) @Max(8) int limit) {
        public SearchRequest {
            excludedTags = excludedTags == null ? List.of() : List.copyOf(excludedTags);
        }
    }
}
