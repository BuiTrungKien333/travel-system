package com.travel.planning.controller;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.common.response.ApiResponse;
import com.travel.place.entity.Place;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.algorithm.model.RouteLeg;
import com.travel.planning.algorithm.model.TspRequest;
import com.travel.planning.algorithm.model.TspResult;
import com.travel.planning.dto.request.OptimizeRouteRequest;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.dto.response.TspLegResponse;
import com.travel.planning.dto.response.TspLocationResponse;
import com.travel.planning.dto.response.TspOptimizeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/public/planning/tsp", "/api/v1/planning/tsp"})
@RequiredArgsConstructor
@Slf4j(topic = "TSP_OPTIMIZATION_CONTROLLER")
@Tag(name = "Planning - TSP Optimization", description = "API tối ưu hóa lộ trình di chuyển du lịch theo thuật toán Held-Karp")
public class TspOptimizationController {

    private final HeldKarpSolver heldKarpSolver;

    @PostMapping("/optimize")
    @Operation(summary = "Tối ưu hóa thứ tự các địa điểm du lịch bằng thuật toán Held-Karp")
    public ResponseEntity<ApiResponse<TspOptimizeResponse>> optimizeRoute(
            @Valid @RequestBody OptimizeRouteRequest request
    ) {
        log.info("Received TSP route optimization request with {} locations, roundTrip={}",
                request.locations().size(), request.isRoundTrip());

        // 1. Chuyển đổi danh sách DTO thành List<Place>
        List<Place> places = new ArrayList<>();
        for (TspLocationRequest loc : request.locations()) {
            UUID placeId = loc.id() != null ? loc.id() : UUID.randomUUID();
            places.add(Place.builder()
                    .id(placeId)
                    .name(loc.name())
                    .latitude(loc.latitude())
                    .longitude(loc.longitude())
                    .build());
        }

        // 2. Xác định điểm bắt đầu (startPlaceId)
        UUID startPlaceId = request.startPlaceId();
        if (startPlaceId == null && request.startIndex() != null) {
            int idx = request.startIndex();
            if (idx < 0 || idx >= places.size()) {
                throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
            }
            startPlaceId = places.get(idx).getId();
        }

        // 3. Xác định điểm kết thúc (endPlaceId nếu đi 1 chiều)
        UUID endPlaceId = request.endPlaceId();
        if (endPlaceId == null && request.endIndex() != null) {
            int idx = request.endIndex();
            if (idx < 0 || idx >= places.size()) {
                throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
            }
            endPlaceId = places.get(idx).getId();
        }

        // 4. Giải bài toán TSP bằng Held-Karp
        TspRequest tspRequest = TspRequest.builder()
                .places(places)
                .startPlaceId(startPlaceId)
                .endPlaceId(endPlaceId)
                .roundTrip(request.isRoundTrip())
                .build();

        TspResult result = heldKarpSolver.solve(tspRequest);

        // 5. Chuyển đổi kết quả sang Response DTO
        List<TspLocationResponse> orderedPlaces = result.getOrderedPlaces().stream()
                .map(this::toLocationResponse)
                .toList();

        List<TspLegResponse> legs = result.getLegs().stream()
                .map(this::toLegResponse)
                .toList();

        double totalDistKm = round(result.getTotalDistanceMeters() / 1000.0, 2);
        long totalDurationMinutes = Math.round(result.getTotalDurationSeconds() / 60.0);

        TspOptimizeResponse response = new TspOptimizeResponse(
                orderedPlaces,
                legs,
                result.getTotalDistanceMeters(),
                totalDistKm,
                result.getTotalDurationSeconds(),
                totalDurationMinutes,
                result.isRoundTrip()
        );

        return ResponseEntity.ok(ApiResponse.success("Tối ưu hóa lộ trình thành công", response));
    }

    private TspLocationResponse toLocationResponse(Place place) {
        return new TspLocationResponse(
                place.getId(),
                place.getName(),
                place.getLatitude(),
                place.getLongitude()
        );
    }

    private TspLegResponse toLegResponse(RouteLeg leg) {
        double distKm = round(leg.getDistanceMeters() / 1000.0, 2);
        long durMinutes = Math.round(leg.getDurationSeconds() / 60.0);

        return new TspLegResponse(
                toLocationResponse(leg.getFromPlace()),
                toLocationResponse(leg.getToPlace()),
                leg.getDistanceMeters(),
                distKm,
                leg.getDurationSeconds(),
                durMinutes
        );
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
