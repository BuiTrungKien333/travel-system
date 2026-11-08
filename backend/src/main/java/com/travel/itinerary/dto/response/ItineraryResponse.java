package com.travel.itinerary.dto.response;

import com.travel.itinerary.dto.DaySettings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Days are grouped by planned date; stops are ordered by their zero-based sequence.
 */
public record ItineraryResponse(
        UUID id,
        UUID tripId,
        String name,
        String status,
        List<ItineraryDayResponse> days,
        Instant updatedAt
) {
    public record ItineraryDayResponse(
            LocalDate date,
            List<ItineraryStopResponse> stops,
            DaySettings settings
    ) {
        public ItineraryDayResponse(LocalDate date, List<ItineraryStopResponse> stops) {
            this(date, stops, null);
        }
    }

    public record ItineraryStopResponse(
            UUID id,
            UUID placeId,
            String name,
            String address,
            Double latitude,
            Double longitude,
            String category,
            LocalDateTime arrivalTime,
            int durationMinutes,
            int sequence,
            String note,
            Double distanceFromPreviousKm,
            Integer travelMinutesFromPrevious
    ) {}
}
