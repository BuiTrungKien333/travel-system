package com.travel.planning.trip.dto;

import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateTripDraftRequest(
    @NotNull UUID clientRequestId,
    @NotNull @Min(0) Long expectedTripVersion,
    @NotNull @Valid GenerateDraftRequest input
) {}
