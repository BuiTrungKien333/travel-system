package com.travel.itinerary.service;

import com.travel.itinerary.dto.request.SaveItineraryRequest;
import com.travel.itinerary.dto.request.ApplyItineraryOptimizationRequest;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.itinerary.dto.response.ItineraryOptimizationResponse;

import java.util.UUID;

public interface ItineraryService {
    ItineraryResponse get(UUID userId, UUID tripId);
    ItineraryResponse save(UUID userId, UUID tripId, SaveItineraryRequest request);
    ItineraryOptimizationResponse optimize(UUID userId, UUID tripId);
    ItineraryResponse applyOptimization(UUID userId, UUID tripId, ApplyItineraryOptimizationRequest request);
}
