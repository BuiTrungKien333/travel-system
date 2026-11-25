package com.travel.planning.retrieval.dto.response;

import com.travel.planning.retrieval.model.PlaceCandidate;
import java.util.List;

public record PlaceSearchResponse(
    String retrievalMode,
    String embeddingModel,
    List<Match> matches,
    String fallbackReason
) {
    public PlaceSearchResponse(String retrievalMode, String embeddingModel, List<Match> matches) {
        this(retrievalMode, embeddingModel, matches, null);
    }

    public record Match(PlaceCandidate place, double rankScore, boolean lexicalMatch, boolean semanticMatch) {}
}
