package com.travel.planning.algorithm;

import com.travel.place.entity.Place;
import com.travel.planning.algorithm.model.DistanceMatrix;
import com.travel.planning.algorithm.model.TspRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HeldKarpOptimalityTest {
    @Test
    void solve_directedCosts_matchesExhaustiveSearchForEndpointModes() {
        Random random = new Random(42);
        for (int n = 3; n <= 7; n++) {
            double[][] costs = new double[n][n];
            List<Place> places = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                places.add(Place.builder().id(new UUID(0, i + 1)).build());
                for (int j = 0; j < n; j++) costs[i][j] = i == j ? 0 : 1 + random.nextInt(100);
            }
            var solver = new HeldKarpSolver(ordered -> new DistanceMatrix(ordered, costs));
            for (boolean fixedStart : List.of(false, true)) {
                for (boolean roundTrip : List.of(false, true)) {
                    for (boolean fixedEnd : List.of(false, true)) {
                        var result = solver.solve(TspRequest.builder().places(places)
                                .startPlaceId(fixedStart ? places.getFirst().getId() : null)
                                .endPlaceId(fixedEnd ? places.getLast().getId() : null)
                                .roundTrip(roundTrip).build());
                        assertThat(result.getTotalDistanceMeters()).as("n=%s, start=%s, round=%s, end=%s",
                                        n, fixedStart, roundTrip, fixedEnd)
                                .isEqualTo(minimumCost(costs, new boolean[n], -1, -1, 0, 0,
                                        fixedStart || roundTrip, roundTrip, fixedEnd && !roundTrip));
                        assertThat(result.getOrderedPlaces().subList(0, n)).containsExactlyInAnyOrderElementsOf(places);
                    }
                }
            }
        }
    }

    @Test
    void solveFixedStart_equalCosts_preservesExistingTieOrder() {
        double[][] costs = {{0, 1, 1, 1}, {1, 0, 1, 1}, {1, 1, 0, 1}, {1, 1, 1, 0}};
        var solver = new HeldKarpSolver(new HaversineDistanceMatrixService());
        assertThat(solver.solveFixedStart(costs, null)).containsExactly(0, 3, 2, 1);
        assertThat(solver.solveFixedStart(costs, 3)).containsExactly(0, 2, 1, 3);
    }

    @Test
    void solveFixedStart_maximumLocations_reconstructsEntirePath() {
        int n = HeldKarpSolver.MAX_LOCATIONS;
        double[][] costs = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) costs[i][j] = j == i + 1 ? 1 : 100;
        }
        assertThat(new HeldKarpSolver(new HaversineDistanceMatrixService()).solveFixedStart(costs, n - 1))
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, n).boxed().toList());
    }

    private double minimumCost(double[][] costs, boolean[] used, int first, int last, int count, double cost,
                               boolean fixedStart, boolean roundTrip, boolean fixedEnd) {
        if (count == costs.length) {
            if (fixedEnd && last != costs.length - 1) return Double.POSITIVE_INFINITY;
            return cost + (roundTrip ? costs[last][first] : 0);
        }
        double minimum = Double.POSITIVE_INFINITY;
        for (int next = 0; next < costs.length; next++) {
            if (used[next] || (count == 0 && fixedStart && next != 0)) continue;
            used[next] = true;
            minimum = Math.min(minimum, minimumCost(costs, used, count == 0 ? next : first, next, count + 1,
                    cost + (count == 0 ? 0 : costs[last][next]), fixedStart, roundTrip, fixedEnd));
            used[next] = false;
        }
        return minimum;
    }
}
