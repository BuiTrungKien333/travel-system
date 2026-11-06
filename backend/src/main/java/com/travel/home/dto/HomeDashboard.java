package com.travel.home.dto;

import com.travel.trip.dto.TripResponses;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HomeDashboard(String state, TripResponses.Read selectedTrip, Integer dayIndex,
        long todayStopCount, Stop nextStop, Integer firstEmptyDay, Footprint footprint,
        List<Photo> recentPhotos, TripResponses.Context context) {
    public record Footprint(long provinces, long places, boolean hasHistory) { }
    public record Stop(UUID id, UUID placeId, UUID itineraryItemId, String name, String address,
                       Double latitude, Double longitude, String category, String time,
                       int durationMinutes, String note) { }
    public record Photo(UUID id, UUID memoryId, UUID tripId, String destination, String placeName,
                        String url, String note, LocalDate date, String time, Instant experiencedAt) { }
}
