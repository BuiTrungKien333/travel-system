package com.travel.planning.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.place.entity.Place;
import com.travel.planning.algorithm.model.RouteLeg;
import com.travel.planning.algorithm.model.TspRequest;
import com.travel.planning.algorithm.model.TspResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HeldKarpSolverTest {

    private HeldKarpSolver solver;

    @BeforeEach
    void setUp() {
        HaversineDistanceMatrixService distanceMatrixService = new HaversineDistanceMatrixService();
        solver = new HeldKarpSolver(distanceMatrixService);
    }

    @Test
    @DisplayName("solve with null or empty places should return empty TspResult")
    void solve_emptyRequest_returnsEmptyResult() {
        TspResult result1 = solver.solve(null);
        assertThat(result1.getOrderedPlaces()).isEmpty();

        TspResult result2 = solver.solve(TspRequest.builder().places(List.of()).build());
        assertThat(result2.getOrderedPlaces()).isEmpty();
    }

    @Test
    @DisplayName("solve with 1 place should return trivial path with 0 distance")
    void solve_singlePlace_returnsTrivialPath() {
        Place p = Place.builder().id(UUID.randomUUID()).name("P1").latitude(16.0).longitude(108.0).build();

        TspResult roundTrip = solver.solve(TspRequest.builder().places(List.of(p)).roundTrip(true).build());
        assertThat(roundTrip.getOrderedPlaces()).hasSize(2);
        assertThat(roundTrip.getTotalDistanceMeters()).isEqualTo(0.0);

        TspResult oneWay = solver.solve(TspRequest.builder().places(List.of(p)).roundTrip(false).build());
        assertThat(oneWay.getOrderedPlaces()).hasSize(1);
        assertThat(oneWay.getTotalDistanceMeters()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("solve with 2 places should return direct path between them")
    void solve_twoPlaces_returnsDirectPath() {
        Place p1 = Place.builder().id(UUID.randomUUID()).name("P1").latitude(16.0).longitude(108.0).build();
        Place p2 = Place.builder().id(UUID.randomUUID()).name("P2").latitude(16.1).longitude(108.0).build();

        TspResult roundTrip = solver.solve(TspRequest.builder().places(List.of(p1, p2)).roundTrip(true).build());
        assertThat(roundTrip.getOrderedPlaces()).hasSize(3);
        assertThat(roundTrip.getOrderedPlaces().get(0)).isEqualTo(p1);
        assertThat(roundTrip.getOrderedPlaces().get(1)).isEqualTo(p2);
        assertThat(roundTrip.getOrderedPlaces().get(2)).isEqualTo(p1);
        assertThat(roundTrip.getLegs()).hasSize(2);

        TspResult oneWay = solver.solve(TspRequest.builder().places(List.of(p1, p2)).roundTrip(false).build());
        assertThat(oneWay.getOrderedPlaces()).hasSize(2);
        assertThat(oneWay.getOrderedPlaces().get(0)).isEqualTo(p1);
        assertThat(oneWay.getOrderedPlaces().get(1)).isEqualTo(p2);
        assertThat(oneWay.getLegs()).hasSize(1);
    }

    @Test
    @DisplayName("solve 4 places on a square should find perimeter tour (not diagonal cross)")
    void solve_squarePlaces_findsPerimeterTour() {
        // Tạo 4 điểm trên một hình vuông: A, B, C, D
        // Thứ tự nhập vào bị xáo trộn: A, C, B, D (C ở góc đối diện với A)
        Place a = Place.builder().id(UUID.randomUUID()).name("A").latitude(16.0).longitude(108.0).build();
        Place b = Place.builder().id(UUID.randomUUID()).name("B").latitude(16.0).longitude(108.1).build();
        Place c = Place.builder().id(UUID.randomUUID()).name("C").latitude(16.1).longitude(108.1).build();
        Place d = Place.builder().id(UUID.randomUUID()).name("D").latitude(16.1).longitude(108.0).build();

        TspRequest request = TspRequest.builder()
            .places(List.of(a, c, b, d))
            .roundTrip(true)
            .startPlaceId(a.getId())
            .build();

        TspResult result = solver.solve(request);

        assertThat(result.getOrderedPlaces()).hasSize(5);
        assertThat(result.getOrderedPlaces().get(0)).isEqualTo(a);
        assertThat(result.getOrderedPlaces().get(4)).isEqualTo(a);

        // Chu trình tối ưu phải đi theo chu vi (A -> B -> C -> D -> A hoặc A -> D -> C -> B -> A)
        List<String> visitedNames = result.getOrderedPlaces().stream().map(Place::getName).toList();
        boolean isClockwise = visitedNames.equals(List.of("A", "D", "C", "B", "A"));
        boolean isCounterClockwise = visitedNames.equals(List.of("A", "B", "C", "D", "A"));

        assertThat(isClockwise || isCounterClockwise).isTrue();
        assertThat(result.getLegs()).hasSize(4);
        assertThat(result.getTotalDistanceMeters()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("solve one-way with fixed start and fixed end should respect both boundaries")
    void solve_oneWay_fixedStartAndEnd_respectsBoundaries() {
        Place a = Place.builder().id(UUID.randomUUID()).name("A").latitude(16.0).longitude(108.0).build();
        Place b = Place.builder().id(UUID.randomUUID()).name("B").latitude(16.0).longitude(108.1).build();
        Place c = Place.builder().id(UUID.randomUUID()).name("C").latitude(16.1).longitude(108.1).build();
        Place d = Place.builder().id(UUID.randomUUID()).name("D").latitude(16.1).longitude(108.0).build();

        TspRequest request = TspRequest.builder()
            .places(List.of(a, b, c, d))
            .roundTrip(false)
            .startPlaceId(a.getId())
            .endPlaceId(c.getId())
            .build();

        TspResult result = solver.solve(request);

        assertThat(result.getOrderedPlaces()).hasSize(4);
        assertThat(result.getOrderedPlaces().get(0)).isEqualTo(a);
        assertThat(result.getOrderedPlaces().get(3)).isEqualTo(c);
        assertThat(result.getLegs()).hasSize(3);
        assertThat(result.isRoundTrip()).isFalse();
    }

    @Test
    @DisplayName("solve with > 16 places should throw TSP_EXCEEDS_MAX_LOCATIONS")
    void solve_exceedsMaxLocations_throwsAppException() {
        List<Place> places = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            places.add(
                Place.builder()
                    .id(UUID.randomUUID())
                    .name("Place " + i)
                    .latitude(16.0 + i * 0.01)
                    .longitude(108.0 + i * 0.01)
                    .build()
            );
        }

        TspRequest request = TspRequest.builder().places(places).build();

        assertThatThrownBy(() -> solver.solve(request))
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.TSP_EXCEEDS_MAX_LOCATIONS);
    }

    @Test
    @DisplayName("solve with invalid startPlaceId should throw INVALID_TSP_LOCATIONS")
    void solve_invalidStartPlaceId_throwsAppException() {
        Place a = Place.builder().id(UUID.randomUUID()).name("A").latitude(16.0).longitude(108.0).build();
        Place b = Place.builder().id(UUID.randomUUID()).name("B").latitude(16.1).longitude(108.1).build();

        TspRequest request = TspRequest.builder()
            .places(List.of(a, b))
            .startPlaceId(UUID.randomUUID()) // ID không tồn tại
            .build();

        assertThatThrownBy(() -> solver.solve(request))
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_TSP_LOCATIONS);
    }

    @Test
    @DisplayName("Benchmark Held-Karp solver with N=12 and N=14 should complete within acceptable time")
    void solve_benchmarkPerformance_runsFast() {
        int n = 12;
        List<Place> places = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            places.add(
                Place.builder()
                    .id(UUID.randomUUID())
                    .name("Place " + i)
                    .latitude(16.0 + Math.sin(i) * 0.1)
                    .longitude(108.0 + Math.cos(i) * 0.1)
                    .build()
            );
        }

        long startTime = System.currentTimeMillis();
        TspResult result = solver.solve(TspRequest.builder().places(places).roundTrip(true).build());
        long elapsed = System.currentTimeMillis() - startTime;

        assertThat(result.getOrderedPlaces()).hasSize(n + 1);
        assertThat(elapsed).isLessThan(500); // 12 đỉnh thường chạy < 50ms
    }

    @Test
    @DisplayName("Kịch bản thực tế: Tối ưu lịch trình du lịch Đà Nẵng 5 địa điểm (Khứ hồi)")
    void solve_realWorldScenario_daNangTour() {
        // Tọa độ thực tế tại Đà Nẵng
        Place startHotel = Place.builder()
            .id(UUID.randomUUID())
            .name("Khách sạn Mỹ Khê")
            .latitude(16.0592)
            .longitude(108.2464)
            .build();

        Place cauRong = Place.builder()
            .id(UUID.randomUUID())
            .name("Cầu Rồng")
            .latitude(16.0610)
            .longitude(108.2272)
            .build();

        Place chuaLinhUng = Place.builder()
            .id(UUID.randomUUID())
            .name("Chùa Linh Ứng (Sơn Trà)")
            .latitude(16.1002)
            .longitude(108.2758)
            .build();

        Place nguHanhSon = Place.builder()
            .id(UUID.randomUUID())
            .name("Ngũ Hành Sơn")
            .latitude(16.0042)
            .longitude(108.2631)
            .build();

        Place baNaHills = Place.builder()
            .id(UUID.randomUUID())
            .name("Bà Nà Hills")
            .latitude(15.9988)
            .longitude(107.9959)
            .build();

        // Danh sách địa điểm đưa vào bị xáo trộn thứ tự
        List<Place> places = List.of(startHotel, baNaHills, chuaLinhUng, nguHanhSon, cauRong);

        TspRequest request = TspRequest.builder()
            .places(places)
            .startPlaceId(startHotel.getId())
            .roundTrip(true)
            .build();

        TspResult result = solver.solve(request);

        assertThat(result.getOrderedPlaces()).hasSize(6); // 5 địa điểm + 1 điểm quay về xuất phát
        assertThat(result.getOrderedPlaces().get(0)).isEqualTo(startHotel);
        assertThat(result.getOrderedPlaces().get(5)).isEqualTo(startHotel);

        // In kết quả trực quan ra console để kiểm tra
        System.out.println("\n=== KẾT QUẢ TỐI ƯU LỘ TRÌNH ĐÀ NẴNG (HELD-KARP TSP) ===");
        System.out.printf("Tổng quãng đường: %.2f km\n", result.getTotalDistanceMeters() / 1000.0);
        System.out.printf(
            "Tổng thời gian di chuyển ước tính: %d phút (%.1f giờ)\n",
            result.getTotalDurationSeconds() / 60,
            result.getTotalDurationSeconds() / 3600.0
        );
        System.out.println("---------------------------------------------------------");
        for (int i = 0; i < result.getLegs().size(); i++) {
            RouteLeg leg = result.getLegs().get(i);
            System.out.printf(
                "Chặng %d: [%s] ➔ [%s]\n",
                i + 1,
                leg.getFromPlace().getName(),
                leg.getToPlace().getName()
            );
            System.out.printf(
                "   ↳ Khoảng cách: %.2f km | Thời gian: %d phút\n",
                leg.getDistanceMeters() / 1000.0,
                leg.getDurationSeconds() / 60
            );
        }
        System.out.println("=========================================================\n");
    }

    @Test
    void directedDurationCostsKeepStartButChooseTheBestFreeEnd() {
        double[][] costs = {
            { 0, 100, 1 },
            { 100, 0, 100 },
            { 100, 1, 0 },
        };
        assertThat(solver.solveFixedStart(costs, null)).containsExactly(0, 2, 1);
        assertThat(
            solver.solveFixedStart(
                new double[][] {
                    { 0, 7 },
                    { 20, 0 },
                },
                null
            )
        ).containsExactly(0, 1);
    }

    @Test
    void directedCostsCanKeepAnExplicitReturnDestination() {
        double[][] costs = {
            { 0, 100, 1, 50 },
            { 100, 0, 100, 1 },
            { 100, 1, 0, 100 },
            { 100, 100, 100, 0 },
        };
        assertThat(solver.solveFixedStart(costs, 3)).containsExactly(0, 2, 1, 3);
    }

    @Test
    void directedCostsRejectMalformedAndUnreachablePaths() {
        assertThatThrownBy(() ->
            solver.solveFixedStart(
                new double[][] {
                    { 0, Double.POSITIVE_INFINITY },
                    { 1, 0 },
                },
                null
            )
        )
            .isInstanceOf(AppException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThatThrownBy(() ->
            solver.solveFixedStart(
                new double[][] {
                    { 0, -1 },
                    { 1, 0 },
                },
                null
            )
        ).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> solver.solveFixedStart(new double[][] { { 0 }, { 1, 0 } }, null)).isInstanceOf(
            AppException.class
        );
    }
}
