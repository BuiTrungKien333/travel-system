package com.travel.planning.algorithm;

import com.travel.common.exception.AppException;
import com.travel.place.entity.Place;
import com.travel.planning.algorithm.model.TspRequest;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class HeldKarpPinnedTest {
    private final HaversineDistanceMatrixService matrix = new HaversineDistanceMatrixService();
    private final HeldKarpSolver solver = new HeldKarpSolver(matrix);

    private List<Place> places(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i -> Place.builder().id(UUID.randomUUID())
                .name("P" + i).latitude(11.94 + (i * 7 % 11) * 0.01).longitude(108.4 + i * 0.01).build()).toList();
    }

    @Test void fixedSlotsMatchTheExactBruteForceMinimumForOpenAndClosedRoutes() {
        var places = places(6);
        Map<Integer, UUID> fixed = Map.of(1, places.get(4).getId(), 4, places.get(0).getId());
        double[][] distances = matrix.calculateMatrix(places).getMatrix();
        for (boolean closed : List.of(false, true)) {
            var result = solver.solve(TspRequest.builder().places(places).roundTrip(closed).fixedPositions(fixed).build());
            assertThat(result.getOrderedPlaces().get(1)).isEqualTo(places.get(4));
            assertThat(result.getOrderedPlaces().get(4)).isEqualTo(places.get(0));
            double expected = exhaustive(new ArrayList<>(), new boolean[6], places, fixed, distances, closed);
            assertThat(result.getTotalDistanceMeters()).isCloseTo(expected, within(0.000001));
        }
    }

    private double exhaustive(List<Integer> route, boolean[] used, List<Place> places,
            Map<Integer, UUID> fixed, double[][] costs, boolean closed) {
        if (route.size() == places.size()) {
            double cost = 0;
            for (int i = 1; i < route.size(); i++) cost += costs[route.get(i - 1)][route.get(i)];
            return cost + (closed ? costs[route.getLast()][route.getFirst()] : 0);
        }
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < places.size(); i++) {
            if (used[i] || (fixed.containsKey(route.size()) && !fixed.get(route.size()).equals(places.get(i).getId()))) continue;
            used[i] = true; route.add(i);
            best = Math.min(best, exhaustive(route, used, places, fixed, costs, closed));
            route.removeLast(); used[i] = false;
        }
        return best;
    }

    @Test void twoVisitsHonorThePinInsteadOfTheSmallInputShortcut() {
        var places = places(2);
        var result = solver.solve(TspRequest.builder().places(places).roundTrip(false)
                .fixedPositions(Map.of(0, places.get(1).getId())).build());
        assertThat(result.getOrderedPlaces()).containsExactly(places.get(1), places.get(0));
    }

    @Test void allPinnedVisitsAndAnExplicitStartRemainInTheirPositions() {
        var places = places(4);
        var result = solver.solve(TspRequest.builder().places(places).roundTrip(false).startPlaceId(places.get(2).getId())
                .fixedPositions(Map.of(0, places.get(2).getId(), 1, places.get(3).getId(), 2, places.get(0).getId(), 3, places.get(1).getId())).build());
        assertThat(result.getOrderedPlaces()).containsExactly(places.get(2), places.get(3), places.get(0), places.get(1));
    }

    @Test void impossibleOrMalformedPinsFailInsteadOfReturningAnIncompleteRoute() {
        var places = places(3);
        for (var fixed : List.of(Map.of(-1, places.get(0).getId()), Map.of(3, places.get(0).getId()),
                Map.of(1, UUID.randomUUID()), Map.of(0, places.get(0).getId(), 1, places.get(0).getId()))) {
            assertThatThrownBy(() -> solver.solve(TspRequest.builder().places(places).roundTrip(false).fixedPositions(fixed).build()))
                    .isInstanceOf(AppException.class);
        }
        assertThatThrownBy(() -> solver.solve(TspRequest.builder().places(places).roundTrip(false)
                .startPlaceId(places.get(0).getId()).fixedPositions(Map.of(1, places.get(0).getId())).build()))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> solver.solve(TspRequest.builder().places(places).roundTrip(false)
                .endPlaceId(places.get(0).getId()).fixedPositions(Map.of(0, places.get(0).getId())).build()))
                .isInstanceOf(AppException.class);
    }
}
