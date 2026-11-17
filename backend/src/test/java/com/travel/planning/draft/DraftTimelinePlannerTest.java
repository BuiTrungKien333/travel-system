package com.travel.planning.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planning.algorithm.HaversineDistanceMatrixService;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.draft.dto.response.DraftResponse;
import com.travel.planning.draft.service.DraftTimelinePlanner;
import com.travel.planning.timeline.TimelineEngine;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static com.travel.planning.draft.DraftFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class DraftTimelinePlannerTest {
    final DraftTimelinePlanner planner = new DraftTimelinePlanner(new HeldKarpSolver(new HaversineDistanceMatrixService()), new TimelineEngine(), new ObjectMapper());

    @Test void includesOriginAndReturnAndPreservesAllVisitIds() {
        var points = List.of(candidate(1), candidate(2));
        var result = planner.build(1, DATE, LocalTime.of(8,0), LocalTime.of(17,0), ORIGIN, true, 45, 10, points, entities(points), travel(points));
        assertThat(result.stops()).extracting(s -> s.place().placeId()).containsExactlyInAnyOrder(points.get(0).placeId(), points.get(1).placeId());
        assertThat(result.returnToOrigin()).isNotNull(); assertThat(result.finish()).isEqualTo(result.returnToOrigin().arrival());
        assertThat(result.totalTransitMinutes()).isEqualTo(result.stops().stream().mapToInt(DraftResponse.Stop::transitMinutes).sum() + result.returnToOrigin().minutes());
        assertThat(result.blocked()).isFalse(); assertThat(result.totalDistanceKm()).isPositive();
    }

    @Test void waitingActuallyDelaysDepartureAndFollowingStop() {
        var point = candidate(1); var db = entities(List.of(point));
        db.get(point.placeId()).setOpeningTime(LocalTime.of(10,0)); db.get(point.placeId()).setClosingTime(LocalTime.of(17,0));
        var result = planner.build(1, DATE, LocalTime.of(8,0), LocalTime.of(17,0), ORIGIN, true, 0, 0, List.of(point), db, travel(List.of(point)));
        var stop = result.stops().getFirst();
        assertThat(stop.waitingMinutes()).isPositive();
        assertThat(stop.visitStart()).isEqualTo(DATE.atTime(10,0)); assertThat(stop.departure()).isEqualTo(DATE.atTime(11,0));
        assertThat(result.finish()).isAfter(stop.departure());
    }

    @Test void restAndBuffersAreCountedInNextDeparture() {
        var points = List.of(candidate(1), candidate(2), candidate(3));
        var day = planner.build(1, DATE, LocalTime.of(8,0), LocalTime.of(17,0), ORIGIN, false, 45, 15, points, entities(points), travel(points));
        assertThat(day.pauses()).filteredOn(p -> p.type().equals("REST")).hasSize(1);
        assertThat(day.pauses()).filteredOn(p -> p.type().equals("BUFFER")).hasSize(2);
        assertThat(Duration.between(day.stops().get(1).departure(), day.stops().get(2).transitDeparture()).toMinutes()).isEqualTo(60);
        assertThat(day.returnToOrigin()).isNull();
    }

    @Test void flagsTooShortDayAndUnknownHoursWithoutClaimingVerified() {
        var point = candidate(1);
        var result = planner.build(1, DATE, LocalTime.of(8,0), LocalTime.of(8,30), ORIGIN, true, 0, 0, List.of(point), entities(List.of(point)), travel(List.of(point)));
        assertThat(result.blocked()).isTrue();
        assertThat(result.issues()).extracting(DraftResponse.Problem::code).contains("DAY_END_EXCEEDED", "UNVERIFIED_HOURS", "APPROXIMATE_COORDINATES");
    }

    @Test void visitAfterClosingOrCrossingClosingIsBlocking() {
        var point = candidate(1); var db = entities(List.of(point));
        db.get(point.placeId()).setOpeningTime(LocalTime.of(8,0)); db.get(point.placeId()).setClosingTime(LocalTime.of(8,30));
        var day = planner.build(1, DATE, LocalTime.of(8,0), LocalTime.of(17,0), ORIGIN, false, 0, 0, List.of(point), db, travel(List.of(point)));
        assertThat(day.issues()).anyMatch(p -> p.code().equals("CLOSING_TIME") && p.blocking());
    }

    @Test void emptyDayHasNoFabricatedVisitsOrTravel() {
        var day = planner.build(2, DATE, LocalTime.of(8,0), LocalTime.of(17,0), ORIGIN, true, 45, 15, List.of(), Map.of(), travel(List.of()));
        assertThat(day.stops()).isEmpty(); assertThat(day.totalDistanceKm()).isZero(); assertThat(day.finish()).isEqualTo(day.start());
        assertThat(day.issues()).extracting(DraftResponse.Problem::code).contains("EMPTY_DAY");
    }
}
