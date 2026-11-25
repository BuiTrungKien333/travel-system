package com.travel.itinerary.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ItineraryDayRequest(
    @NotNull LocalDate date,
    @NotNull @Size(max = 100) List<@Valid ItineraryStopRequest> stops
) {}
