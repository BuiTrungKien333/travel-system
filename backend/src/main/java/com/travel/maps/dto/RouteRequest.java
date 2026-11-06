package com.travel.maps.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RouteRequest(
        @NotNull @Valid RoutePoint origin,
        @NotNull @Valid RoutePoint destination,
        @Size(max = 23) List<@NotNull @Valid RoutePoint> intermediates,
        TravelMode travelMode
) {
    public enum TravelMode { DRIVE, WALK, BICYCLE, TWO_WHEELER }
}
