package com.travel.maps.service;

import static com.travel.maps.service.MapboxDirectionsService.Mode.DRIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.travel.maps.dto.RoutePoint;
import com.travel.maps.repository.TripRouteRepository;
import com.travel.planning.algorithm.HaversineDistanceMatrixService;
import com.travel.planning.algorithm.HeldKarpSolver;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TripDayRoutingServiceTest {

    private final TripRouteRepository repository = mock(TripRouteRepository.class);
    private final MapboxDirectionsService directions = mock(MapboxDirectionsService.class);
    private final TripDayRoutingService service = new TripDayRoutingService(
        repository,
        directions,
        Clock.fixed(Instant.parse("2026-11-10T05:00:00Z"), ZoneOffset.UTC),
        new HeldKarpSolver(new HaversineDistanceMatrixService())
    );
    private final UUID owner = UUID.randomUUID(),
        trip = UUID.randomUUID();
    private final LocalDate date = LocalDate.of(2026, 11, 10);

    private List<TripRouteRepository.Waypoint> prepare(boolean visited) {
        var points = new ArrayList<TripRouteRepository.Waypoint>();
        for (int i = 0; i < 4; i++) points.add(
            new TripRouteRepository.Waypoint(
                UUID.randomUUID(),
                new RoutePoint(null, 16.0 + i * .01, 108.0),
                visited && (i == 0 || i == 2)
            )
        );
        when(repository.ownedDates(trip, owner)).thenReturn(
            Optional.of(new TripRouteRepository.Dates(date, date, "IN_PROGRESS"))
        );
        when(repository.orderedPoints(trip, owner, date)).thenReturn(points);
        when(repository.settings(trip, owner, date)).thenReturn(Optional.empty());
        return points;
    }

    private double[][] matrix(int size) {
        var values = new double[size][size];
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) values[i][j] = i == j ? 0 : 20;
        return values;
    }

    @Test
    void comparesBothOrdersOnTheSameRoadMatrix() {
        var points = prepare(false);
        var costs = matrix(4);
        costs[0][1] = 5;
        costs[1][2] = 5;
        costs[2][3] = 5;
        costs[0][2] = 1;
        costs[2][1] = 1;
        costs[1][3] = 1;
        when(directions.matrix(anyList(), eq(DRIVE))).thenReturn(new MapboxDirectionsService.RoadMatrix(costs, costs));
        var result = service.optimize(owner, trip, date, DRIVE);
        assertThat(result.order()).containsExactly(
            points.get(0).placeId(),
            points.get(2).placeId(),
            points.get(1).placeId(),
            points.get(3).placeId()
        );
        assertThat(result.originalDistanceMeters()).isEqualTo(15);
        assertThat(result.distanceMeters()).isEqualTo(3);
    }

    @Test
    void fasterButLongerCandidateKeepsOriginalOrder() {
        var points = prepare(false);
        var durations = matrix(4);
        durations[0][2] = 1;
        durations[2][1] = 1;
        durations[1][3] = 1;
        var distances = matrix(4);
        distances[0][1] = 1;
        distances[1][2] = 1;
        distances[2][3] = 1;
        when(directions.matrix(anyList(), eq(DRIVE))).thenReturn(
            new MapboxDirectionsService.RoadMatrix(distances, durations)
        );
        var result = service.optimize(owner, trip, date, DRIVE);
        assertThat(result.order()).containsExactlyElementsOf(
            points.stream().map(TripRouteRepository.Waypoint::placeId).toList()
        );
        assertThat(result.distanceMeters()).isEqualTo(result.originalDistanceMeters());
        assertThat(result.durationSeconds()).isEqualTo(result.originalDurationSeconds());
    }

    @Test
    void visitedACStayAheadAndOnlyPendingBDAreOptimizedFromC() {
        var points = prepare(true);
        var costs = matrix(3);
        costs[0][2] = 1;
        costs[2][1] = 1;
        when(directions.matrix(anyList(), eq(DRIVE))).thenReturn(new MapboxDirectionsService.RoadMatrix(costs, costs));
        var result = service.optimize(owner, trip, date, DRIVE);
        assertThat(result.order()).containsExactly(
            points.get(0).placeId(),
            points.get(2).placeId(),
            points.get(3).placeId(),
            points.get(1).placeId()
        );
        assertThat(result.visitedOrder()).containsExactly(points.get(0).placeId(), points.get(2).placeId());
        assertThat(result.originalDistanceMeters()).isEqualTo(40);
        assertThat(result.distanceMeters()).isEqualTo(2);
    }

    @Test
    void returnToOriginIncludesBothOriginLegsWithoutAddingAnExtraStop() {
        var points = prepare(false);
        var origin = new com.travel.itinerary.dto.DaySettings.Origin("Hotel", "Address", 16.0, 108.0);
        when(repository.settings(trip, owner, date)).thenReturn(
            Optional.of(
                new com.travel.itinerary.dto.DaySettings(
                    origin,
                    LocalTime.of(8, 0),
                    true,
                    com.travel.itinerary.dto.DaySettings.TravelMode.DRIVE
                )
            )
        );
        var costs = matrix(6);
        when(directions.matrix(anyList(), eq(DRIVE))).thenAnswer(invocation -> {
            List<RoutePoint> locations = invocation.getArgument(0);
            assertThat(locations).hasSize(6);
            assertThat(locations.getFirst()).isEqualTo(locations.getLast());
            return new MapboxDirectionsService.RoadMatrix(costs, costs);
        });
        var result = service.optimize(owner, trip, date, DRIVE);
        assertThat(result.order()).containsExactlyInAnyOrderElementsOf(
            points.stream().map(TripRouteRepository.Waypoint::placeId).toList()
        );
        assertThat(result.originalDistanceMeters()).isEqualTo(100);
        assertThat(result.distanceMeters()).isEqualTo(100);
    }
}
