package com.travel.itinerary.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Full replacement of the itinerary's planned days and stops. */
public record SaveItineraryRequest(@NotNull @Size(max = 90) List<@Valid ItineraryDayRequest> days) {}
