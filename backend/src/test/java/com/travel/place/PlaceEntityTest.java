package com.travel.place;

import com.travel.place.entity.Place;
import com.travel.place.entity.Province;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceEntityTest {

    @Test
    @DisplayName("Province entity should hold correct fields and satisfy equals/hashCode contract")
    void provinceEntity_shouldHoldFieldsAndSatisfyContract() {
        UUID provinceId = UUID.randomUUID();
        Province province1 = Province.builder()
                .id(provinceId)
                .code("VN-48")
                .name("Đà Nẵng")
                .build();

        Province province2 = Province.builder()
                .id(provinceId)
                .code("VN-48")
                .name("Đà Nẵng")
                .build();

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
        Province province = Province.builder()
                .id(UUID.randomUUID())
                .code("VN-48")
                .name("Đà Nẵng")
                .build();

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
    }
}
