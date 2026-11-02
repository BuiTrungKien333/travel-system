package com.travel.planning.ai.intent.model;

import java.util.List;
import java.util.Map;

/** Extracted preferences, not a persisted itinerary or verified place identifiers. */
public record TravelIntent(String destination, Integer numberOfDays, List<String> interests,
        List<String> excludedTags, String dailyStartTime, String dailyEndTime, String travelPace,
        List<String> mustVisitNames, List<String> excludedPlaceNames, Map<String, String> evidence) {}
