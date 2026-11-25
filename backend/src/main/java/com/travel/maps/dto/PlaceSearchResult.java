package com.travel.maps.dto;

public record PlaceSearchResult(
    String providerPlaceId,
    String name,
    String address,
    Double latitude,
    Double longitude,
    String category
) {}
