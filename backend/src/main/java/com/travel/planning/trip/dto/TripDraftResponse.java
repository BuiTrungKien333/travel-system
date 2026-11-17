package com.travel.planning.trip.dto;

import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.dto.response.DraftResponse;
import java.time.Instant;
import java.util.UUID;

public record TripDraftResponse(UUID id, UUID tripId, Instant expiresAt, boolean applied,
        boolean canApply, GenerateDraftRequest input, DraftResponse proposal) {}
