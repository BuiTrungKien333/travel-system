package com.travel.planning.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanceCalculatorTest {

    @Test
    @DisplayName("calculateDistanceMeters should return 0 when two points are identical")
    void calculateDistanceMeters_identicalPoints_returnsZero() {
        double dist = DistanceCalculator.calculateDistanceMeters(16.0614, 108.2272, 16.0614, 108.2272);
        assertThat(dist).isEqualTo(0.0);
    }

    @Test
    @DisplayName("calculateDistanceMeters between Dragon Bridge and Ba Na Hills should be around 25-28 km straight line")
    void calculateDistanceMeters_dragonBridgeToBaNaHills_returnsAccurateDistance() {
        // Cầu Rồng: 16.0614, 108.2272
        // Bà Nà Hills: 15.9988, 107.9961
        double distMeters = DistanceCalculator.calculateDistanceMeters(16.0614, 108.2272, 15.9988, 107.9961);

        // Khoảng cách đường chim bay thực tế xấp xỉ 25.7 km (25,700m)
        assertThat(distMeters).isCloseTo(25670.0, within(1000.0));
    }

    @Test
    @DisplayName("calculateDurationSeconds should estimate duration accurately based on speed")
    void calculateDurationSeconds_estimatesAccurateTime() {
        // 35 km ở vận tốc 35 km/h -> chính xác 1 giờ (3600 giây)
        double distanceMeters = 35000.0;
        long durationSeconds = DistanceCalculator.calculateDurationSeconds(distanceMeters, 35.0);

        assertThat(durationSeconds).isEqualTo(3600L);
    }

    @Test
    @DisplayName("calculateDurationSeconds with default speed should use 35 km/h")
    void calculateDurationSeconds_defaultSpeed_uses35KmPerHour() {
        double distanceMeters = 17500.0; // 17.5 km -> 0.5 giờ (1800 giây)
        long durationSeconds = DistanceCalculator.calculateDurationSeconds(distanceMeters);

        assertThat(durationSeconds).isEqualTo(1800L);
    }

    @Test
    @DisplayName("calculateDurationSeconds with zero or negative distance should return 0")
    void calculateDurationSeconds_zeroDistance_returnsZero() {
        assertThat(DistanceCalculator.calculateDurationSeconds(0.0)).isEqualTo(0L);
        assertThat(DistanceCalculator.calculateDurationSeconds(-100.0)).isEqualTo(0L);
    }
}
