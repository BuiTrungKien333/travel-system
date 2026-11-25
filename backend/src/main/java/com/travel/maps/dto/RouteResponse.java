package com.travel.maps.dto;

public record RouteResponse(int distanceMeters, long durationSeconds, String encodedPolyline, String provider) {}
