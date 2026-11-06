package com.travel.trip.service;

import com.travel.trip.dto.request.*;
import com.travel.trip.dto.response.*;
import java.util.List;
import java.util.UUID;

public interface TripService {

    java.util.List<TripPlaceResponse> addPlaces(UUID userId, UUID tripId, AddTripPlacesRequest request);
    TripResponse create(UUID userId, CreateTripRequest request);
    List<TripResponse> list(UUID userId);
    TripResponse get(UUID userId, UUID tripId);
    TripResponse update(UUID userId, UUID tripId, UpdateTripRequest request);
    void delete(UUID userId, UUID tripId);
    TripPlaceResponse addPlace(UUID userId, UUID tripId, AddTripPlaceRequest request);
    List<TripPlaceResponse> listPlaces(UUID userId, UUID tripId);
    TripPlaceResponse updatePlace(UUID userId, UUID tripId, UUID placeId, UpdateTripPlaceRequest request);
    void removePlace(UUID userId, UUID tripId, UUID placeId);
}
