package com.travel.trip.service;

import com.travel.trip.dto.request.AddTripPlaceRequest;
import com.travel.trip.dto.request.AddTripPlacesRequest;
import com.travel.trip.dto.request.UpdateTripPlaceRequest;
import com.travel.trip.dto.response.TripPlaceResponse;

import java.util.List;
import java.util.UUID;

public interface TripService {

    List<TripPlaceResponse> addPlaces(UUID userId, UUID tripId, AddTripPlacesRequest request);

    TripPlaceResponse addPlace(UUID userId, UUID tripId, AddTripPlaceRequest request);

    List<TripPlaceResponse> listPlaces(UUID userId, UUID tripId);

    TripPlaceResponse updatePlace(UUID userId, UUID tripId, UUID placeId, UpdateTripPlaceRequest request);

    void removePlace(UUID userId, UUID tripId, UUID placeId);
}
