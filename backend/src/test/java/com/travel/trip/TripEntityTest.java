package com.travel.trip;

import static org.assertj.core.api.Assertions.assertThat;

import com.travel.place.entity.Place;
import com.travel.trip.entity.Trip;
import com.travel.trip.entity.TripPlace;
import com.travel.trip.enums.TripStatus;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TripEntityTest {

    @Test
    @DisplayName("Trip entity should hold fields, status, and support soft delete correctly")
    void tripEntity_shouldHoldFieldsAndSupportSoftDelete() {
        User user = User.builder().fullName("Nguyen Van A").email("user@travel.com").role(Role.USER).build();

        Trip trip = Trip.builder()
            .user(user)
            .name("Chuyến đi Đà Nẵng 3N2Đ")
            .destination("Đà Nẵng")
            .startDate(LocalDate.of(2026, 9, 10))
            .endDate(LocalDate.of(2026, 9, 12))
            .preferences("Thích thiên nhiên, ẩm thực địa phương")
            .constraints("Không đi bộ quá 5km mỗi ngày")
            .status(TripStatus.PLANNING)
            .build();

        assertThat(trip.getName()).isEqualTo("Chuyến đi Đà Nẵng 3N2Đ");
        assertThat(trip.getDestination()).isEqualTo("Đà Nẵng");
        assertThat(trip.getStatus()).isEqualTo(TripStatus.PLANNING);
        assertThat(trip.getUser()).isEqualTo(user);
        assertThat(trip.isDeleted()).isFalse();

        // Soft delete
        trip.softDelete();
        assertThat(trip.isDeleted()).isTrue();
        assertThat(trip.getDeletedAt()).isNotNull();

        // Restore
        trip.restore();
        assertThat(trip.isDeleted()).isFalse();
        assertThat(trip.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("TripPlace entity should link Trip and Place with plannedOrder and duration")
    void tripPlaceEntity_shouldLinkTripAndPlaceWithMetadata() {
        UUID tripPlaceId = UUID.randomUUID();
        Trip trip = Trip.builder().name("Trip 1").build();
        Place place = Place.builder().name("Cầu Vàng").latitude(15.9988).longitude(107.9961).build();

        TripPlace tripPlace = TripPlace.builder()
            .id(tripPlaceId)
            .trip(trip)
            .place(place)
            .priority(1)
            .estimatedDuration(120)
            .notes("Check-in chụp ảnh buổi sáng")
            .plannedOrder(1)
            .visited(false)
            .build();

        assertThat(tripPlace.getId()).isEqualTo(tripPlaceId);
        assertThat(tripPlace.getTrip()).isEqualTo(trip);
        assertThat(tripPlace.getPlace()).isEqualTo(place);
        assertThat(tripPlace.getPriority()).isEqualTo(1);
        assertThat(tripPlace.getEstimatedDuration()).isEqualTo(120);
        assertThat(tripPlace.getPlannedOrder()).isEqualTo(1);
        assertThat(tripPlace.isVisited()).isFalse();

        tripPlace.setVisited(true);
        assertThat(tripPlace.isVisited()).isTrue();
    }
}
