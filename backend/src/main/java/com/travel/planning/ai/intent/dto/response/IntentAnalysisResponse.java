package com.travel.planning.ai.intent.dto.response;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.retrieval.model.PlaceCandidate;

import java.util.*;

public record IntentAnalysisResponse(String status, TravelIntent intent, String provinceCode, List<String> missingFields,
            List<String> questions, List<String> warnings, String positiveQuery, List<PlaceCandidate> mustVisitPlaces,
            List<UUID> excludedPlaceIds, String model) {}
