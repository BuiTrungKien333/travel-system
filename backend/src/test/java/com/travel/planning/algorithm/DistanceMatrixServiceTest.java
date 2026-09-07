package com.travel.planning.algorithm;

import com.travel.place.entity.Place;
import com.travel.planning.algorithm.model.DistanceMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceMatrixServiceTest {

    private DistanceMatrixService distanceMatrixService;

    @BeforeEach
    void setUp() {
        distanceMatrixService = new HaversineDistanceMatrixService();
    }

    @Test
    @DisplayName("calculateMatrix should handle empty places list gracefully")
    void calculateMatrix_emptyList_returnsEmptyMatrix() {
        DistanceMatrix matrix = distanceMatrixService.calculateMatrix(List.of());

        assertThat(matrix.size()).isEqualTo(0);
        assertThat(matrix.getMatrix()).isEmpty();
    }

    @Test
    @DisplayName("calculateMatrix should return symmetrical matrix with zeros on diagonal")
    void calculateMatrix_validPlaces_returnsSymmetricalMatrix() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Place p1 = Place.builder().id(id1).name("P1").latitude(16.0).longitude(108.0).build();
        Place p2 = Place.builder().id(id2).name("P2").latitude(16.1).longitude(108.1).build();
        Place p3 = Place.builder().id(id3).name("P3").latitude(16.2).longitude(108.2).build();

        List<Place> places = List.of(p1, p2, p3);

        DistanceMatrix matrix = distanceMatrixService.calculateMatrix(places);

        assertThat(matrix.size()).isEqualTo(3);

        // Kiểm tra đường chéo chính bằng 0
        assertThat(matrix.getDistance(0, 0)).isEqualTo(0.0);
        assertThat(matrix.getDistance(1, 1)).isEqualTo(0.0);
        assertThat(matrix.getDistance(2, 2)).isEqualTo(0.0);

        // Kiểm tra tính đối xứng
        assertThat(matrix.getDistance(0, 1)).isEqualTo(matrix.getDistance(1, 0));
        assertThat(matrix.getDistance(0, 2)).isEqualTo(matrix.getDistance(2, 0));
        assertThat(matrix.getDistance(1, 2)).isEqualTo(matrix.getDistance(2, 1));

        // Kiểm tra các giá trị khoảng cách dương
        assertThat(matrix.getDistance(0, 1)).isGreaterThan(0.0);
        assertThat(matrix.getDistance(1, 2)).isGreaterThan(0.0);

        // Kiểm tra lookup index qua UUID
        assertThat(matrix.indexOf(id1)).contains(0);
        assertThat(matrix.indexOf(id2)).contains(1);
        assertThat(matrix.indexOf(id3)).contains(2);
        assertThat(matrix.indexOf(UUID.randomUUID())).isEmpty();
    }
}
