package com.travel.planning.draft;

import com.travel.place.entity.Place;
import com.travel.place.enums.PlaceType;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.model.DraftTravelMatrix;
import com.travel.planning.retrieval.model.PlaceCandidate;

import java.time.*;
import java.util.*;

final class DraftFixtures {
    static final LocalDate DATE = LocalDate.of(2026, 11, 2);
    static final GenerateDraftRequest.Origin ORIGIN = new GenerateDraftRequest.Origin("Điểm xuất phát demo", 16.065, 108.223);
    static PlaceCandidate candidate(int n) {
        return new PlaceCandidate(new UUID(0, n), "Bảo tàng " + n, "Di sản văn hóa", List.of("MUSEUM", "CULTURE"),
                16.06 + n * .002, 108.22 + n * .002, "INDOOR", null, null, 60,
                "https://example.com/curated", DATE, "DESCRIPTION_ONLY", "APPROXIMATE_DEMO", "UNKNOWN", 1);
    }
    static Place entity(PlaceCandidate p) {
        return Place.builder().id(p.placeId()).name(p.name()).latitude(p.latitude()).longitude(p.longitude())
                .placeType(PlaceType.INDOOR).openingTime(p.openingTime()).closingTime(p.closingTime()).build();
    }
    static Map<UUID, Place> entities(List<PlaceCandidate> candidates) {
        var map = new HashMap<UUID, Place>(); candidates.forEach(p -> map.put(p.placeId(), entity(p))); return map;
    }
    static DraftTravelMatrix travel(List<PlaceCandidate> candidates) {
        return DraftTravelMatrix.estimated(ORIGIN, candidates, "TEST_ESTIMATE");
    }
}
