package com.travel.planning.algorithm;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.place.entity.Place;
import com.travel.planning.algorithm.model.TspRequest;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.enums.PlanningTravelMode;
import com.travel.planning.timeline.TimelineEngine;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class TravelModeTimingTest {
    private final HeldKarpSolver solver = new HeldKarpSolver(new HaversineDistanceMatrixService());

    @Test void aRoundTripFromAVisitCoordinateKeepsThePinnedVisitAndIncludesTheReturnLeg() {
        var day = LocalDate.of(2026,11,20);
        var origin = new TspLocationRequest(UUID.randomUUID(),"Hotel",11.94,108.44);
        var first = new TspLocationRequest(UUID.randomUUID(),"First",11.94,108.44,null,null,null,30,null,null,0,day.atTime(8,0));
        var second = new TspLocationRequest(UUID.randomUUID(),"Second",11.95,108.45);
        var rules = org.mockito.Mockito.mock(com.travel.planning.rule.service.RuleEvaluatorService.class);
        org.mockito.Mockito.when(rules.evaluateItinerary(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(com.travel.planning.rule.model.ItineraryEvaluationResult.of(List.of()));
        var service = new com.travel.planning.service.TripOptimizationServiceImpl(solver,
                org.mockito.Mockito.mock(com.travel.weather.service.WeatherService.class), rules,
                org.mockito.Mockito.mock(com.travel.planning.ai.provider.AiProviderFactory.class),
                org.mockito.Mockito.mock(com.travel.place.repository.PlaceRepository.class));
        var round = service.optimizeAndAuditItinerary(new com.travel.planning.dto.request.OptimizeAndAuditRequest(
                "Da Lat",day,LocalTime.of(8,0),null,origin,true,List.of(first,second),null,PlanningTravelMode.WALK));
        var open = service.optimizeAndAuditItinerary(new com.travel.planning.dto.request.OptimizeAndAuditRequest(
                "Da Lat",day,LocalTime.of(8,0),null,origin,false,List.of(first,second),null,PlanningTravelMode.WALK));
        assertThat(round.schedule()).extracting(com.travel.planning.rule.model.ScheduleItemContext::placeId)
                .containsExactly(first.id(),second.id());
        assertThat(round.schedule().getFirst().arrivalTime()).isEqualTo(day.atTime(8,0));
        assertThat(round.totalDistanceKm()).isGreaterThan(open.totalDistanceKm());
        assertThat(round.totalTransitMinutes()).isGreaterThan(open.totalTransitMinutes());
    }

    @Test void bothSmallAndGeneralRoutesUseTheSelectedTravelMode() {
        for (int count : List.of(2, 4)) {
            var places = java.util.stream.IntStream.range(0,count).mapToObj(i -> Place.builder().id(UUID.randomUUID())
                    .name("Visit "+i).latitude(11.94+i*0.01).longitude(108.44).build()).toList();
            for (boolean roundTrip : List.of(false,true)) {
                var drive = solver.solve(TspRequest.builder().places(places).startPlaceId(places.getFirst().getId()).roundTrip(roundTrip)
                        .travelMode(PlanningTravelMode.DRIVE).build());
                var walk = solver.solve(TspRequest.builder().places(places).startPlaceId(places.getFirst().getId()).roundTrip(roundTrip)
                        .travelMode(PlanningTravelMode.WALK).build());
                var motorcycle = solver.solve(TspRequest.builder().places(places).startPlaceId(places.getFirst().getId()).roundTrip(roundTrip)
                        .travelMode(PlanningTravelMode.TWO_WHEELER).build());
                assertThat(walk.getOrderedPlaces()).isEqualTo(drive.getOrderedPlaces());
                assertThat(walk.getTotalDistanceMeters()).isEqualTo(drive.getTotalDistanceMeters());
                assertThat(walk.getTotalDurationSeconds()).isGreaterThan(drive.getTotalDurationSeconds());
                assertThat(motorcycle.getTotalDurationSeconds()).isEqualTo(drive.getTotalDurationSeconds());
            }
        }
    }

    @Test void theOriginLegUsesTheModeBeforeCheckingAPinnedAppointment() {
        var day = LocalDate.of(2026,11,20);
        var origin = new TspLocationRequest(UUID.randomUUID(),"Origin",0.0,0.0);
        var visit = Place.builder().id(UUID.randomUUID()).name("Pinned").latitude(0.1).longitude(0.0).build();
        var pinned = new TspLocationRequest(visit.getId(),"Pinned",0.1,0.0,null,null,null,30,null,null,0,day.atTime(10,0));
        var timeline = new TimelineEngine();
        var drive = timeline.buildTimelineForDay(day,LocalTime.of(8,0),origin,List.of(visit),Map.of(visit.getId(),pinned),null,PlanningTravelMode.DRIVE);
        assertThat(drive.getFirst().arrivalTime()).isEqualTo(day.atTime(10,0));
        assertThatThrownBy(() -> timeline.buildTimelineForDay(day,LocalTime.of(8,0),origin,List.of(visit),Map.of(visit.getId(),pinned),null,PlanningTravelMode.WALK))
                .isInstanceOfSatisfying(AppException.class,error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ITINERARY_PIN_CONFLICT));
    }
}
