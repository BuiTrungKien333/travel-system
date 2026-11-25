package com.travel.planning.retrieval.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PlaceCandidate(
    UUID placeId,
    String name,
    String description,
    List<String> tags,
    double latitude,
    double longitude,
    String placeType,
    LocalTime openingTime,
    LocalTime closingTime,
    int estimatedDurationMinutes,
    String sourceUrl,
    LocalDate reviewedOn,
    String verificationScope,
    String coordinatesQuality,
    String hoursQuality,
    int contentVersion
) {
    public String document() {
        return name + "\n" + description + "\n" + String.join(", ", tags);
    }
}
