package com.travel.planning.algorithm;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.place.entity.Place;
import com.travel.planning.algorithm.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "HELD_KARP_SOLVER")
public class HeldKarpSolver {

    public static final int MAX_LOCATIONS = 16;
    private final DistanceMatrixService distanceMatrixService;

    /**
     * Giải bài toán Người du lịch (TSP) bằng thuật toán Quy hoạch động Held-Karp
     *
     * @param request {@link TspRequest} chứa danh sách địa điểm và tùy chọn ràng buộc
     * @return {@link TspResult} chứa thứ tự tối ưu và chi tiết các chặng
     */
    public TspResult solve(TspRequest request) {
        if (request == null || request.getPlaces() == null || request.getPlaces().isEmpty()) {
            return TspResult.builder().build();
        }

        List<Place> inputPlaces = request.getPlaces();
        int n = inputPlaces.size();

        if (n > MAX_LOCATIONS) {
            log.warn("[HELD_KARP] Locations count {} exceeds maximum allowed limit {}", n, MAX_LOCATIONS);
            throw new AppException(ErrorCode.TSP_EXCEEDS_MAX_LOCATIONS);
        }

        // Xử lý thứ tự places để đưa startPlace lên đầu (index 0) nếu có
        List<Place> places = arrangePlacesWithStartNode(inputPlaces, request.getStartPlaceId());

        // Kiểm tra endPlaceId nếu có
        Integer fixedEndIndex = null;
        if (request.getEndPlaceId() != null) {
            fixedEndIndex = findPlaceIndex(places, request.getEndPlaceId());
            if (fixedEndIndex == null) {
                log.warn("[HELD_KARP] Specified endPlaceId {} not found in places list", request.getEndPlaceId());
                throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
            }
        }

        // Xử lý các trường hợp biên N = 1, N = 2
        if (n == 1) {
            return handleSinglePlace(places.get(0), request.isRoundTrip());
        }
        if (n == 2) {
            return handleTwoPlaces(places.get(0), places.get(1), request.isRoundTrip(), fixedEndIndex);
        }

        // Xây dựng ma trận khoảng cách N x N
        DistanceMatrix distanceMatrix = distanceMatrixService.calculateMatrix(places);
        double[][] dist = distanceMatrix.getMatrix();

        log.debug("[HELD_KARP] Starting Held-Karp solver for N={}, roundTrip={}", n, request.isRoundTrip());

        int totalSubsets = 1 << n;
        double[][] dp = new double[totalSubsets][n];
        int[][] parent = new int[totalSubsets][n];

        for (int mask = 0; mask < totalSubsets; mask++) {
            Arrays.fill(dp[mask], Double.POSITIVE_INFINITY);
            Arrays.fill(parent[mask], -1);
        }

        boolean startFixed = (request.getStartPlaceId() != null) || request.isRoundTrip();

        // Khởi tạo trạng thái cơ sở
        if (startFixed) {
            // Cố định đỉnh xuất phát tại index 0 (với Round-trip, chu trình khép kín bất biến với điểm bắt đầu)
            dp[1][0] = 0.0;
        } else {
            // Điểm xuất phát tự do trong bài toán một chiều
            for (int i = 0; i < n; i++) {
                dp[1 << i][i] = 0.0;
            }
        }

        // Duyệt quy hoạch động qua các tập con
        for (int mask = 1; mask < totalSubsets; mask++) {
            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) == 0 || Double.isInfinite(dp[mask][u])) {
                    continue;
                }

                for (int v = 0; v < n; v++) {
                    if ((mask & (1 << v)) == 0) {
                        int nextMask = mask | (1 << v);
                        double newCost = dp[mask][u] + dist[u][v];
                        if (newCost < dp[nextMask][v]) {
                            dp[nextMask][v] = newCost;
                            parent[nextMask][v] = u;
                        }
                    }
                }
            }
        }

        int fullMask = totalSubsets - 1;
        List<Integer> optimalPathIndices;

        if (request.isRoundTrip()) {
            optimalPathIndices = reconstructRoundTripPath(n, fullMask, dp, parent, dist);
        } else {
            optimalPathIndices = reconstructOneWayPath(n, fullMask, dp, parent, fixedEndIndex);
        }

        return buildTspResult(places, optimalPathIndices, dist, request.isRoundTrip());
    }

    private List<Place> arrangePlacesWithStartNode(List<Place> originalPlaces, UUID startPlaceId) {
        if (startPlaceId == null) {
            return new ArrayList<>(originalPlaces);
        }

        List<Place> arranged = new ArrayList<>(originalPlaces.size());
        Place startPlace = null;

        for (Place place : originalPlaces) {
            if (place.getId().equals(startPlaceId)) {
                startPlace = place;
            } else {
                arranged.add(place);
            }
        }

        if (startPlace == null) {
            log.warn("[HELD_KARP] Specified startPlaceId {} not found in places list", startPlaceId);
            throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
        }

        arranged.add(0, startPlace);
        return arranged;
    }

    private Integer findPlaceIndex(List<Place> places, UUID placeId) {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).getId().equals(placeId)) {
                return i;
            }
        }
        return null;
    }

    private List<Integer> reconstructRoundTripPath(int n, int fullMask, double[][] dp, int[][] parent, double[][] dist) {
        double minTotalCost = Double.POSITIVE_INFINITY;
        int bestEndNode = -1;

        for (int u = 1; u < n; u++) {
            double totalCost = dp[fullMask][u] + dist[u][0];
            if (totalCost < minTotalCost) {
                minTotalCost = totalCost;
                bestEndNode = u;
            }
        }

        List<Integer> path = backtrackPath(bestEndNode, fullMask, parent);
        path.add(0); // Khép vòng quay lại điểm xuất phát index 0
        return path;
    }

    private List<Integer> reconstructOneWayPath(int n, int fullMask, double[][] dp, int[][] parent, Integer fixedEndIndex) {
        int bestEndNode;

        if (fixedEndIndex != null) {
            bestEndNode = fixedEndIndex;
        } else {
            double minTotalCost = Double.POSITIVE_INFINITY;
            bestEndNode = 0;
            for (int u = 0; u < n; u++) {
                if (dp[fullMask][u] < minTotalCost) {
                    minTotalCost = dp[fullMask][u];
                    bestEndNode = u;
                }
            }
        }

        return backtrackPath(bestEndNode, fullMask, parent);
    }

    private List<Integer> backtrackPath(int endNode, int fullMask, int[][] parent) {
        List<Integer> path = new ArrayList<>();
        int curr = endNode;
        int mask = fullMask;

        while (curr != -1) {
            path.add(curr);
            int prev = parent[mask][curr];
            mask = mask ^ (1 << curr);
            curr = prev;
        }

        Collections.reverse(path);
        return path;
    }

    private TspResult buildTspResult(List<Place> places, List<Integer> pathIndices, double[][] dist, boolean isRoundTrip) {
        List<Place> orderedPlaces = new ArrayList<>(pathIndices.size());
        for (int idx : pathIndices) {
            orderedPlaces.add(places.get(idx));
        }

        List<RouteLeg> legs = new ArrayList<>();
        double totalDistance = 0.0;
        long totalDuration = 0L;

        for (int i = 0; i < pathIndices.size() - 1; i++) {
            int fromIdx = pathIndices.get(i);
            int toIdx = pathIndices.get(i + 1);

            double legDistance = dist[fromIdx][toIdx];
            long legDuration = DistanceCalculator.calculateDurationSeconds(legDistance);

            legs.add(RouteLeg.builder()
                    .fromPlace(places.get(fromIdx))
                    .toPlace(places.get(toIdx))
                    .distanceMeters(legDistance)
                    .durationSeconds(legDuration)
                    .build());

            totalDistance += legDistance;
            totalDuration += legDuration;
        }

        return TspResult.builder()
                .orderedPlaces(orderedPlaces)
                .totalDistanceMeters(totalDistance)
                .totalDurationSeconds(totalDuration)
                .legs(legs)
                .roundTrip(isRoundTrip)
                .build();
    }

    private TspResult handleSinglePlace(Place place, boolean isRoundTrip) {
        List<Place> ordered = isRoundTrip ? List.of(place, place) : List.of(place);
        List<RouteLeg> legs = isRoundTrip ? List.of(RouteLeg.builder()
                .fromPlace(place)
                .toPlace(place)
                .distanceMeters(0.0)
                .durationSeconds(0L)
                .build()) : Collections.emptyList();

        return TspResult.builder()
                .orderedPlaces(ordered)
                .totalDistanceMeters(0.0)
                .totalDurationSeconds(0L)
                .legs(legs)
                .roundTrip(isRoundTrip)
                .build();
    }

    private TspResult handleTwoPlaces(Place p0, Place p1, boolean isRoundTrip, Integer fixedEndIndex) {
        double dist = DistanceCalculator.calculateDistanceMeters(
                p0.getLatitude(), p0.getLongitude(),
                p1.getLatitude(), p1.getLongitude()
        );
        long dur = DistanceCalculator.calculateDurationSeconds(dist);

        if (fixedEndIndex != null && fixedEndIndex == 0) {
            // Điểm kết thúc là p0 -> lộ trình p1 -> p0
            Place temp = p0;
            p0 = p1;
            p1 = temp;
        }

        if (isRoundTrip) {
            return TspResult.builder()
                    .orderedPlaces(List.of(p0, p1, p0))
                    .totalDistanceMeters(dist * 2.0)
                    .totalDurationSeconds(dur * 2)
                    .legs(List.of(
                            RouteLeg.builder().fromPlace(p0).toPlace(p1).distanceMeters(dist).durationSeconds(dur).build(),
                            RouteLeg.builder().fromPlace(p1).toPlace(p0).distanceMeters(dist).durationSeconds(dur).build()
                    ))
                    .roundTrip(true)
                    .build();
        } else {
            return TspResult.builder()
                    .orderedPlaces(List.of(p0, p1))
                    .totalDistanceMeters(dist)
                    .totalDurationSeconds(dur)
                    .legs(List.of(
                            RouteLeg.builder().fromPlace(p0).toPlace(p1).distanceMeters(dist).durationSeconds(dur).build()
                    ))
                    .roundTrip(false)
                    .build();
        }
    }
}
