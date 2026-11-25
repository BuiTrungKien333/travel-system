package com.travel.planning.draft;

import static com.travel.planning.draft.DraftFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.*;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.maps.service.MapboxDirectionsService.RoadMatrix;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.algorithm.*;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.dto.response.DraftResponse.*;
import com.travel.planning.draft.model.*;
import com.travel.planning.draft.service.*;
import com.travel.planning.timeline.TimelineEngine;
import java.util.*;
import org.junit.jupiter.api.Test;

class DraftRoadSchedulingTest {

    final MapboxDirectionsService maps = mock(MapboxDirectionsService.class);
    final DraftTimelinePlanner planner = new DraftTimelinePlanner(
        new HeldKarpSolver(new HaversineDistanceMatrixService()),
        new TimelineEngine(),
        new ObjectMapper()
    );
    final DraftSchedulingService scheduling = new DraftSchedulingService(planner, maps);
    final List<com.travel.planning.retrieval.model.PlaceCandidate> points = List.of(candidate(1), candidate(2));

    @Test
    void directedRoadTimesDetermineOrderAndTimelineIncludingOriginAndReturn() {
        when(maps.matrix(anyList(), any())).thenReturn(
            new RoadMatrix(
                new double[][] {
                    { 0, 6000, 1200 },
                    { 2500, 0, 500 },
                    { 4000, 1700, 0 },
                },
                new double[][] {
                    { 0, 600, 60.2 },
                    { 120.1, 0, 60 },
                    { 600, 65, 0 },
                }
            )
        );
        var schedule = schedule(true, "17:00", Set.of(), List.of(ids()));
        var day = schedule.days().getFirst();
        assertThat(schedule.routeSource()).isEqualTo("MAPBOX_DRIVING");
        assertThat(day.stops())
            .extracting(s -> s.place().placeId())
            .containsExactly(points.get(1).placeId(), points.get(0).placeId());
        assertThat(day.stops().getFirst().arrival()).isEqualTo(DATE.atTime(8, 2));
        assertThat(day.stops().getFirst().distanceKm()).isEqualTo(1.2);
        assertThat(day.stops().get(1).transitMinutes()).isEqualTo(2);
        assertThat(day.returnToOrigin().minutes()).isEqualTo(3);
        assertThat(day.totalTransitMinutes()).isEqualTo(7);
        assertThat(day.totalDistanceKm()).isEqualTo(5.4);
        verify(maps, times(1)).matrix(argThat(p -> p.size() == 3), eq(MapboxDirectionsService.Mode.DRIVE));
    }

    @Test
    void oneWayDoesNotIncludeAnUnrequestedReturnLeg() {
        when(maps.matrix(anyList(), any())).thenReturn(
            new RoadMatrix(
                new double[][] {
                    { 0, 2000, 500 },
                    { 1000, 0, 400 },
                    { 6000, 700, 0 },
                },
                new double[][] {
                    { 0, 1200, 60 },
                    { 600, 0, 60 },
                    { 3600, 120, 0 },
                }
            )
        );
        var day = schedule(false, "17:00", Set.of(), List.of(ids())).days().getFirst();
        assertThat(day.returnToOrigin()).isNull();
        assertThat(day.totalTransitMinutes()).isEqualTo(3);
        assertThat(day.totalDistanceKm()).isEqualTo(1.2);
        assertThat(day.stops().getFirst().place().placeId()).isEqualTo(points.get(1).placeId());
    }

    @Test
    void providerFailureUsesAnExplicitWholeRequestEstimate() {
        when(maps.matrix(anyList(), any())).thenThrow(new AppException(ErrorCode.PLACE_PROVIDER_UNAVAILABLE));
        var schedule = schedule(true, "17:00", Set.of(), List.of(ids()));
        assertThat(schedule.routeSource()).isEqualTo("ESTIMATED_HAVERSINE_35_KMH");
        assertThat(schedule.fallbackReason()).isEqualTo("PLACE_PROVIDER_UNAVAILABLE");
        assertThat(schedule.days().getFirst().stops()).hasSize(2);
        verify(maps, times(1)).matrix(anyList(), any());
    }

    @Test
    void nullRoadPairsAreNotReplacedWithStraightLineTravel() {
        double inf = Double.POSITIVE_INFINITY;
        when(maps.matrix(anyList(), any())).thenReturn(
            new RoadMatrix(
                new double[][] {
                    { 0, inf, inf },
                    { inf, 0, inf },
                    { inf, inf, 0 },
                },
                new double[][] {
                    { 0, inf, inf },
                    { inf, 0, inf },
                    { inf, inf, 0 },
                }
            )
        );
        var schedule = schedule(true, "17:00", new HashSet<>(ids()), List.of(ids()));
        assertThat(schedule.routeSource()).isEqualTo("MAPBOX_DRIVING");
        assertThat(schedule.days().getFirst().issues()).anyMatch(
            p -> p.blocking() && p.code().equals("ROUTE_UNAVAILABLE")
        );
        assertThat(schedule.days().getFirst().stops()).isEmpty();
        assertThat(schedule.unassigned())
            .hasSize(2)
            .allMatch(p -> p.reason().equals("REQUIRED_PLACE_UNREACHABLE"));
    }

    @Test
    void providerNoRouteDoesNotSilentlyFallBackToAnEstimate() {
        when(maps.matrix(anyList(), any())).thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        var schedule = schedule(true, "17:00", new HashSet<>(ids()), List.of(ids()));
        assertThat(schedule.fallbackReason()).isEqualTo("NO_ROAD_ROUTE");
        assertThat(schedule.days().getFirst().blocked()).isTrue();
        assertThat(schedule.days().getFirst().returnToOrigin()).isNull();
    }

    @Test
    void removingAllOptionalUnreachableStopsCannotHideTheRoadFailure() {
        when(maps.matrix(anyList(), any())).thenThrow(new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        var schedule = schedule(true, "17:00", Set.of(), List.of(ids()));
        assertThat(schedule.days().getFirst().blocked()).isTrue();
        assertThat(schedule.unassigned()).allMatch(p -> p.reason().equals("ROAD_ROUTE_UNAVAILABLE"));
    }

    @Test
    void realTravelOverflowPreservesRequiredPlacesAsAVisibleConflictAndReusesMatrixForRepair() {
        when(maps.matrix(anyList(), any())).thenReturn(
            new RoadMatrix(
                new double[][] {
                    { 0, 10000, 10000 },
                    { 10000, 0, 10000 },
                    { 10000, 10000, 0 },
                },
                new double[][] {
                    { 0, 3600, 3600 },
                    { 3600, 0, 3600 },
                    { 3600, 3600, 0 },
                }
            )
        );
        var schedule = schedule(true, "10:30", new HashSet<>(ids()), List.of(ids()));
        assertThat(schedule.days().getFirst().issues()).anyMatch(p -> p.code().equals("DAY_END_EXCEEDED"));
        assertThat(schedule.days().getFirst().stops()).hasSize(2);
        verify(maps, times(1)).matrix(anyList(), any());
    }

    @Test
    void multipleDaysAndRepairShareOneMatrix() {
        when(maps.matrix(anyList(), any())).thenReturn(
            new RoadMatrix(
                new double[][] {
                    { 0, 10000, 10000 },
                    { 10000, 0, 10000 },
                    { 10000, 10000, 0 },
                },
                new double[][] {
                    { 0, 1800, 1800 },
                    { 1800, 0, 1800 },
                    { 1800, 1800, 0 },
                }
            )
        );
        var schedule = schedule(true, "10:30", Set.of(), List.of(ids(), List.of()));
        assertThat(schedule.days())
            .hasSize(2)
            .allMatch(d -> !d.blocked());
        assertThat(schedule.repairPasses()).isPositive();
        verify(maps, times(1)).matrix(anyList(), any());
    }

    private List<UUID> ids() {
        return points
            .stream()
            .map(p -> p.placeId())
            .toList();
    }

    private DraftSchedulingService.Schedule schedule(
        boolean roundTrip,
        String end,
        Set<UUID> required,
        List<List<UUID>> groups
    ) {
        var request = new GenerateDraftRequest("fixture", DATE, ORIGIN, roundTrip, 0);
        var intent = new TravelIntent(
            "Đà Nẵng",
            groups.size(),
            List.of("CULTURE"),
            List.of(),
            "08:00",
            end,
            "BALANCED",
            List.of(),
            List.of(),
            Map.of()
        );
        return scheduling.schedule(
            request,
            intent,
            new DraftAllocation(groups, Map.of(), "AI", null),
            points,
            entities(points),
            required
        );
    }
}
