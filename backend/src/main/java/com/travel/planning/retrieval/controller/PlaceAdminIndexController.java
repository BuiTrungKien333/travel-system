package com.travel.planning.retrieval.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.retrieval.dto.response.PlaceIndexResponse;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({ "dev", "rag-demo", "planning-ai" })
@RequestMapping("/api/v1/admin/planning/retrieval")
public class PlaceAdminIndexController {

    private final PlaceRetrievalService service;

    public PlaceAdminIndexController(PlaceRetrievalService service) {
        this.service = service;
    }

    @PostMapping("/index")
    @Operation(
        summary = "Index approved places for RAG; administrator only",
        description = "Skips current embeddings. Provider failures are reported as failed counts; never updates unapproved places."
    )
    public ApiResponse<PlaceIndexResponse> index() {
        return ApiResponse.success(service.index());
    }

    @PostMapping("/search")
    @Operation(
        summary = "Test hybrid RAG retrieval as an administrator",
        description = "Runs lexical and semantic place retrieval and reports which source matched each candidate."
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
