package com.travel.itinerary.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.itinerary.dto.DaySettings;
import com.travel.itinerary.dto.request.SaveItineraryDayRequest;
import com.travel.itinerary.dto.request.UpdateDayScheduleRequest;
import com.travel.itinerary.dto.response.ItineraryResponse;
import com.travel.maps.dto.TripDayRoute;
import com.travel.maps.service.MapboxDirectionsService;
import com.travel.place.repository.PlaceRepository;
import com.travel.trip.entity.Trip;
import com.travel.trip.enums.TripStatus;
import com.travel.trip.repository.TripPlaceRepository;
import com.travel.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DaySchedulePinsTest {
    @Mock ItineraryService itineraries;
    @Mock TripRepository trips;
    @Mock TripPlaceRepository memberships;
    @Mock PlaceRepository places;
    @Mock MapboxDirectionsService directions;
    @Mock Trip trip;
    UUID owner = UUID.randomUUID(), tripId = UUID.randomUUID(), place = UUID.randomUUID();
    LocalDate date = LocalDate.of(2027, 1, 1);
    Instant revision = Instant.parse("2026-11-08T00:00:00Z");
    DayScheduleService service;

    @BeforeEach void setup() {
        when(trips.findByIdAndUserIdAndDeletedAtIsNull(tripId, owner)).thenReturn(Optional.of(trip));
        when(trip.getStartDate()).thenReturn(date);
        when(trip.getEndDate()).thenReturn(date);
        when(trip.getStatus()).thenReturn(TripStatus.PLANNING);
        when(trip.getId()).thenReturn(tripId);
        var stop = new ItineraryResponse.ItineraryStopResponse(UUID.randomUUID(), place, "Pinned", null, 16.0,
                108.0, null, date.atTime(8, 30), 60, 0, null, null, null, true);
        when(itineraries.get(owner, tripId)).thenReturn(new ItineraryResponse(UUID.randomUUID(), tripId, "Day",
                "DRAFT", List.of(new ItineraryResponse.ItineraryDayResponse(date, List.of(stop))), revision));
        when(directions.route(anyList(), any())).thenReturn(TripDayRoute.unavailable(TripDayRoute.Status.NOT_CONFIGURED));
        service = new DayScheduleService(itineraries, trips, memberships, places, directions,
                Clock.fixed(revision, ZoneOffset.UTC));
    }

    @Test void updatingDepartureKeepsThePinnedAppointmentAndFlag() {
        service.update(owner, tripId, date, request(LocalTime.of(8, 0)));
        var saved = ArgumentCaptor.forClass(SaveItineraryDayRequest.class);
        verify(itineraries).saveDay(eq(owner), eq(tripId), eq(date), saved.capture());
        assertThat(saved.getValue().stops().getFirst().pinned()).isTrue();
        assertThat(saved.getValue().stops().getFirst().arrivalTime()).isEqualTo(date.atTime(8, 30));
    }

    @Test void departureAfterThePinnedAppointmentCannotOverwriteIt() {
        assertThatThrownBy(() -> service.update(owner, tripId, date, request(LocalTime.of(9, 0))))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ITINERARY_PIN_CONFLICT));
        verify(itineraries, never()).saveDay(any(), any(), any(), any());
    }

    private UpdateDayScheduleRequest request(LocalTime departure) {
        return new UpdateDayScheduleRequest(0L, revision,
                new DaySettings(null, departure, false, DaySettings.TravelMode.DRIVE),
                List.of(new UpdateDayScheduleRequest.VisitDuration(place, 60)));
    }
}
