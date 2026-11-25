package com.travel.place;

import static org.assertj.core.api.Assertions.assertThat;

import com.travel.place.entity.Place;
import com.travel.place.entity.Province;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaceEntityTest {

    @Test
    @DisplayName("Province entity should hold correct fields and satisfy equals/hashCode contract")
    void provinceEntity_shouldHoldFieldsAndSatisfyContract() {
        UUID provinceId = UUID.randomUUID();
        Province province1 = Province.builder().id(provinceId).code("VN-48").name("Đà Nẵng").build();

        Province province2 = Province.builder().id(provinceId).code("VN-48").name("Đà Nẵng").build();

        assertThat(province1.getId()).isEqualTo(provinceId);
        assertThat(province1.getCode()).isEqualTo("VN-48");
        assertThat(province1.getName()).isEqualTo("Đà Nẵng");
        assertThat(province1).isEqualTo(province2);
        assertThat(province1.hashCode()).isEqualTo(province2.hashCode());
    }

    @Test
    @DisplayName("Place entity should hold correct coordinates, province, and timestamps")
    void placeEntity_shouldHoldCoordinatesAndProvince() {
        UUID placeId = UUID.randomUUID();
        Province province = Province.builder().id(UUID.randomUUID()).code("VN-48").name("Đà Nẵng").build();

        Instant now = Instant.now();
        Place place = Place.builder()
            .id(placeId)
            .providerPlaceId("google_place_123")
            .name("Bà Nà Hills")
            .address("Hòa Vang, Đà Nẵng")
            .latitude(15.9988)
            .longitude(107.9961)
            .province(province)
            .createdAt(now)
            .build();

        assertThat(place.getId()).isEqualTo(placeId);
        assertThat(place.getName()).isEqualTo("Bà Nà Hills");
        assertThat(place.getAddress()).isEqualTo("Hòa Vang, Đà Nẵng");
        assertThat(place.getLatitude()).isEqualTo(15.9988);
        assertThat(place.getLongitude()).isEqualTo(107.9961);
        assertThat(place.getProvince()).isEqualTo(province);
        assertThat(place.getCreatedAt()).isEqualTo(now);
        assertThat(place.getPlaceType()).isEqualTo(com.travel.place.enums.PlaceType.UNSPECIFIED);
    }

    @Test
    @DisplayName("Place entity should hold opening_time, closing_time and place_type correctly")
    void placeEntity_shouldHoldContextFields() {
        Place place = Place.builder()
            .name("Bảo tàng Điêu khắc Chăm")
            .latitude(16.0601)
            .longitude(108.2235)
            .openingTime(java.time.LocalTime.of(7, 30))
            .closingTime(java.time.LocalTime.of(17, 0))
            .placeType(com.travel.place.enums.PlaceType.INDOOR)
            .build();

        assertThat(place.getOpeningTime()).isEqualTo(java.time.LocalTime.of(7, 30));
        assertThat(place.getClosingTime()).isEqualTo(java.time.LocalTime.of(17, 0));
        assertThat(place.getPlaceType()).isEqualTo(com.travel.place.enums.PlaceType.INDOOR);
    }
}
