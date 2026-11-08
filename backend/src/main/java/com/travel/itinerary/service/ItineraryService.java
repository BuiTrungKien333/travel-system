package com.travel.itinerary.service;

import com.travel.itinerary.dto.request.SaveItineraryRequest;
import com.travel.itinerary.dto.request.SaveItineraryDayRequest;
import com.travel.itinerary.dto.request.ApplyItineraryOptimizationRequest;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.itinerary.dto.response.ItineraryOptimizationResponse;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import com.travel.trip.entity.Trip;
import com.travel.trip.entity.TripPlace;

public interface ItineraryService {
    ItineraryResponse get(UUID userId, UUID tripId);
    ItineraryResponse save(UUID userId, UUID tripId, SaveItineraryRequest request);
    ItineraryResponse saveDay(UUID userId, UUID tripId, LocalDate date, SaveItineraryDayRequest request);
    void appendPlacesToDay(Trip trip, LocalDate date, List<TripPlace> places);
    ItineraryOptimizationResponse optimize(UUID userId, UUID tripId);
    ItineraryResponse applyOptimization(UUID userId, UUID tripId, ApplyItineraryOptimizationRequest request);
}
