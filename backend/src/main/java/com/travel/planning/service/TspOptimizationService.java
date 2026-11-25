package com.travel.planning.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TSP_OPTIMIZATION_SERVICE")
public class TspOptimizationService {

    private final HeldKarpSolver heldKarpSolver;

    public TspOptimizeResponse optimize(OptimizeRouteRequest request) {
        log.info(
            "Received TSP route optimization request with {} locations, roundTrip={}",
            request.locations().size(),
            request.isRoundTrip()
        );

        // 1. Chuyển đổi danh sách DTO thành List<Place>
        List<Place> places = new ArrayList<>();
        for (TspLocationRequest loc : request.locations()) {
            UUID placeId = loc.id() != null ? loc.id() : UUID.randomUUID();
            places.add(
                Place.builder().id(placeId).name(loc.name()).latitude(loc.latitude()).longitude(loc.longitude()).build()
            );
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
        List<TspLocationResponse> orderedPlaces = result
            .getOrderedPlaces()
            .stream()
            .map(this::toLocationResponse)
            .toList();

        List<TspLegResponse> legs = result.getLegs().stream().map(this::toLegResponse).toList();

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

        return response;
    }

    private TspLocationResponse toLocationResponse(Place place) {
        return new TspLocationResponse(place.getId(), place.getName(), place.getLatitude(), place.getLongitude());
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
