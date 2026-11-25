package com.travel.weather.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Destination-area weather; location identifies the actual coordinate source. */
public record TripWeather(
    LocalDate date,
    LocalDate today,
    LocalDate availableThrough,
    Instant fetchedAt,
    Location location,
    WeatherForecastResult forecast,
    CurrentWeather current,
    String unavailableReason
) {
    public record Location(UUID placeId, String name, double latitude, double longitude, String basis) {}
}
