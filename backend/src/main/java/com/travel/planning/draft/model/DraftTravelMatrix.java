package com.travel.planning.draft.model;

import com.travel.maps.service.MapboxDirectionsService.RoadMatrix;
import com.travel.planning.algorithm.DistanceCalculator;
import com.travel.planning.draft.dto.request.GenerateDraftRequest.Origin;
import com.travel.planning.retrieval.model.PlaceCandidate;
import java.util.*;

/** One request-wide matrix: index zero is the private origin, followed by candidate IDs. */
public record DraftTravelMatrix(Map<UUID, Integer> indices, RoadMatrix matrix, String source, String fallbackReason) {
    public static final String ROAD = "MAPBOX_DRIVING";
    public static final String ESTIMATED = "ESTIMATED_HAVERSINE_35_KMH";

    public static Map<UUID, Integer> indices(List<PlaceCandidate> candidates) {
        Map<UUID, Integer> result = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) result.put(candidates.get(i).placeId(), i + 1);
        return Map.copyOf(result);
    }

    public static DraftTravelMatrix estimated(Origin origin, List<PlaceCandidate> candidates, String reason) {
        int size = candidates.size() + 1;
        double[][] distances = new double[size][size], durations = new double[size][size];
        for (int from = 0; from < size; from++) {
            double lat = from == 0 ? origin.latitude() : candidates.get(from - 1).latitude();
            double lon = from == 0 ? origin.longitude() : candidates.get(from - 1).longitude();
            for (int to = 0; to < size; to++) {
                double toLat = to == 0 ? origin.latitude() : candidates.get(to - 1).latitude();
                double toLon = to == 0 ? origin.longitude() : candidates.get(to - 1).longitude();
                distances[from][to] = DistanceCalculator.calculateDistanceMeters(lat, lon, toLat, toLon);
                durations[from][to] = DistanceCalculator.calculateDurationSeconds(distances[from][to]);
            }
        }
        return new DraftTravelMatrix(indices(candidates), new RoadMatrix(distances, durations), ESTIMATED, reason);
    }

    public List<Integer> dayIndices(List<PlaceCandidate> selected, boolean roundTrip) {
        List<Integer> result = new ArrayList<>();
        result.add(0);
        selected.forEach(p -> result.add(Objects.requireNonNull(indices.get(p.placeId()))));
        if (roundTrip) result.add(0);
        return result;
    }

    public double[][] costs(List<Integer> dayIndices) {
        int size = dayIndices.size();
        double[][] result = new double[size][size];
        for (int from = 0; from < size; from++) for (int to = 0; to < size; to++) {
            int globalFrom = dayIndices.get(from), globalTo = dayIndices.get(to);
            double distance = matrix.distances()[globalFrom][globalTo], duration = matrix.durations()[globalFrom][globalTo];
            result[from][to] = Double.isFinite(distance) && Double.isFinite(duration)
                    ? ROAD.equals(source) ? duration : distance : Double.POSITIVE_INFINITY;
        }
        return result;
    }
}
