package com.travel.planning.algorithm;

import static org.assertj.core.api.Assertions.assertThat;

import com.travel.place.entity.Place;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HaversineDistanceMatrixServiceTest {

    private HaversineDistanceMatrixService distanceMatrixService;

    @BeforeEach
    void setUp() {
        distanceMatrixService = new HaversineDistanceMatrixService();
    }

    @Test
    @DisplayName("calculateMatrix should handle empty places list gracefully")
    void calculateMatrix_emptyList_returnsEmptyMatrix() {
        double[][] matrix = distanceMatrixService.calculateMatrix(List.of());

        assertThat(matrix).isEmpty();
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

        double[][] matrix = distanceMatrixService.calculateMatrix(places);

        assertThat(matrix.length).isEqualTo(3);

        // Kiểm tra đường chéo chính bằng 0
        assertThat(matrix[0][0]).isEqualTo(0.0);
        assertThat(matrix[1][1]).isEqualTo(0.0);
        assertThat(matrix[2][2]).isEqualTo(0.0);

        // Kiểm tra tính đối xứng
        assertThat(matrix[0][1]).isEqualTo(matrix[1][0]);
        assertThat(matrix[0][2]).isEqualTo(matrix[2][0]);
        assertThat(matrix[1][2]).isEqualTo(matrix[2][1]);

        // Kiểm tra các giá trị khoảng cách dương
        assertThat(matrix[0][1]).isGreaterThan(0.0);
        assertThat(matrix[1][2]).isGreaterThan(0.0);
    }
}
