package com.travel.planning.ai.intent.dto.response;

import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;

public record IntentSearchResponse(String status, IntentAnalysisResponse analysis, PlaceSearchResponse candidates) {}
