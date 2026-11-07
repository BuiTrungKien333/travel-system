package com.travel.itinerary.dto;

import com.travel.maps.service.MapboxDirectionsService.Mode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalTime;

/** A private origin snapshot; it does not create a public Place or a trip stop. */
public record DaySettings(@Valid Origin origin, @NotNull LocalTime departureTime,
        boolean returnToStart, @NotNull Mode travelMode) {
    public record Origin(@NotBlank @Size(max = 200) String name, @Size(max = 500) String address,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude) {
        @AssertTrue(message = "Both coordinates must be supplied together")
        public boolean isCoordinatePairValid() {
            return (latitude == null) == (longitude == null);
        }
    }
    @AssertTrue(message = "A return route requires an origin")
    public boolean isReturnValid() { return !returnToStart || origin != null; }
}
