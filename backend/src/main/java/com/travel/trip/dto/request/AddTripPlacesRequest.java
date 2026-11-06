package com.travel.trip.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record AddTripPlacesRequest(@NotEmpty @Size(max = 50) List<@NotNull UUID> placeIds) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unknown batch field"); }
}
