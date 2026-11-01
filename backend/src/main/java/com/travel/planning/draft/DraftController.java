package com.travel.planning.draft;

import com.travel.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@Profile("rag-demo")
@RequestMapping("/api/v1/public/planning")
public class DraftController {
    private final DraftService service;
    public DraftController(DraftService service) { this.service = service; }

    @PostMapping("/generate-draft")
    @Operation(summary = "Preview a 1–2 day itinerary from natural language; does not persist a trip")
    public ApiResponse<DraftModels.Response> generate(@Valid @RequestBody Request request) {
        return ApiResponse.success(service.generate(request));
    }

    public record Origin(@NotBlank @Size(max=160) String name,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {}
    public record Request(@NotBlank @Size(max=1000) String text, @NotNull LocalDate startDate,
            @NotNull @Valid Origin origin, Boolean roundTrip, @Min(0) @Max(90) Integer restMinutes) {
        public Request {
            if (roundTrip == null) roundTrip = true;
            if (restMinutes == null) restMinutes = 45;
        }
    }
}
