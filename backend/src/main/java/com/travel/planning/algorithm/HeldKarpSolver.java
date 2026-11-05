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
    /*
    a & b => and
    a | b => or
    a ^ b => xor, ra 1 khi hai bit khác nhau, như cộng 2 số vậy (101 ^ 011 -> 110)
    1 << k => dịch trái, dịch bit 1 sang trái k vị trí, 1 << 2 -> 100

    1 << v, giá trị là 2 mũ v

    1 << 0 = 0001 (số 1)  đại diện riêng cho thành phố 0.
    1 << 1 = 0010 (số 2)  đại diện riêng cho thành phố 1.
    1 << 2 = 0100 (số 4)  đại diện riêng cho thành phố 2.
    1 << 3 = 1000 (số 8)  đại diện riêng cho thành phố 3.

    mask & (1 << u) (Kiểm tra xem đã đi qua thành phố u chưa)
    Giả sử bạn đã đi qua thành phố 0 và 2 -> mask = 5 (0101)
    kiểm tra thành phố 2:
    0101   (mask)
  & 0100   (1 << 2)
    ------
    0100   (Khác 0 -> Bit 2 đang BẬT -> ĐÃ THĂM)

    kiểm tra thành phố 1:
    0101   (mask)
  & 0010   (1 << 1)
    ------
    0000   (Bằng 0 -> Bit 1 đang TẮT -> CHƯA THĂM)

    (mask & (1 << v)) == 0 nghĩa là chưa đi qua v, có thể chọn v làm điểm kế tiếp.

    mask | (1 << v) (Đánh dấu đã ghé thăm thêm thành phố v)
    Phép | dùng để bật bit ở vị trí v lên 1 mà không làm thay đổi các bit còn lại.

    Ví dụ: Hiện tại đang có mask = 5 (0101, đã thăm 0 và 2). Giờ quyết định đi tiếp sang thành phố 1
    0101   (mask: {0, 2})
  | 0010   (1 << 1: chọn thêm 1)
    ------
    0111   (nextMask: {0, 1, 2}, số 7)

    mask ^ (1 << curr) (Tắt bit của thành phố curr khi truy vết ngược)
    Phép ^ (XOR) với 1 sẽ đảo trạng thái bit từ 1 về 0

    Ví dụ: Khi truy vết ngược từ cuối về đầu, bạn đang ở tập {0, 1, 2} (mask = 7, tức 0111)
    và đỉnh hiện tại là 2. Muốn biết trước đó tập là gì, bạn cần gỡ đỉnh 2 ra khỏi danh sách:

    0111   (mask = 7)
  ^ 0100   (1 << 2)
    ------
    0011   (mask mới = 3, chỉ còn lại {0, 1})

    Tóm tắt:
    1 << k          ---> Biểu diễn riêng thành phố k
    mask & (1 << k) ---> Hỏi: "Thành phố k có trong mask không?" (== 0: Chưa | != 0: Rồi)
    mask | (1 << k) ---> Thêm thành phố k vào hành trình
    mask ^ (1 << k) ---> Bỏ thành phố k ra khỏi hành trình
    (1 << n) - 1    ---> Tất cả n thành phố đều được bật (ví dụ n=3 -> 2^3 - 1 = 7 = 0111)

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
        Map<Integer, UUID> fixed = request.getFixedPositions() == null ? Map.of() : request.getFixedPositions();
        int[] positionByNode = new int[n];
        int[] nodeByPosition = new int[n];
        Arrays.fill(positionByNode, -1);
        Arrays.fill(nodeByPosition, -1);
        for (var entry : fixed.entrySet()) {
            Integer position = entry.getKey();
            Integer node = entry.getValue() == null ? null : findPlaceIndex(places, entry.getValue());
            if (position == null || position < 0 || position >= n || node == null || positionByNode[node] != -1)
                throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
            positionByNode[node] = position;
            nodeByPosition[position] = node;
        }
        // With fixed visit slots, rotating a cycle is no longer equivalent. Compare all permitted starts.
        if (request.isRoundTrip() && request.getStartPlaceId() == null && !fixed.isEmpty()) {
            TspResult best = null;
            for (int node = 0; node < n; node++) {
                if (!allowedPosition(node, 0, positionByNode, nodeByPosition)) continue;
                TspResult result = solve(TspRequest.builder().places(inputPlaces).roundTrip(true)
                        .startPlaceId(places.get(node).getId()).endPlaceId(request.getEndPlaceId()).fixedPositions(fixed).build());
                if (best == null || result.getTotalDistanceMeters() < best.getTotalDistanceMeters()) best = result;
            }
            if (best == null) throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
            return best;
        }

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
            if (!allowedPosition(0, 0, positionByNode, nodeByPosition)) throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
            return handleSinglePlace(places.get(0), request.isRoundTrip());
        }
        if (n == 2 && fixed.isEmpty()) {
            return handleTwoPlaces(places.get(0), places.get(1), request.isRoundTrip(), fixedEndIndex);
        }

        // Xây dựng ma trận khoảng cách N x N
        DistanceMatrix distanceMatrix = distanceMatrixService.calculateMatrix(places);
        double[][] dist = distanceMatrix.getMatrix();

        log.debug("[HELD_KARP] Starting Held-Karp solver for N={}, roundTrip={}", n, request.isRoundTrip());

        int totalSubsets = 1 << n;
        double[][] dp = new double[totalSubsets][n];
        int[][] parent = new int[totalSubsets][n];

        /*
        Điền giá trị dương vô cùng vào tất cả các hàng của dp[mask]
        Vì thuật toán là tìm chi phí nhỏ nhất, nên nếu giá trị hợp lệ thì đều nhỏ hơn dương vô cùng
        kiểu nó bằng dương vô cùng thì nó chưa được đi qua

        Còn bảng parent dùng để lưu truy vết, lưu đỉnh liền trước
        -1 là một chỉ số không tồn tại trong ds đỉnh, kiểu: chưa có đỉnh nào dẫn tới đỉnh này
         */
        for (int mask = 0; mask < totalSubsets; mask++) {
            Arrays.fill(dp[mask], Double.POSITIVE_INFINITY);
            Arrays.fill(parent[mask], -1);
        }

        boolean startFixed = (request.getStartPlaceId() != null) || request.isRoundTrip();

        // Khởi tạo trạng thái cơ sở
        if (startFixed) {
            if (!allowedPosition(0, 0, positionByNode, nodeByPosition)) throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
            // Cố định đỉnh xuất phát tại index 0 (với Round-trip, chu trình khép kín bất biến với điểm bắt đầu)
            dp[1][0] = 0.0;
        } else {
            // Điểm xuất phát tự do trong bài toán một chiều
            for (int i = 0; i < n; i++) {
                if (allowedPosition(i, 0, positionByNode, nodeByPosition)) dp[1 << i][i] = 0.0;
            }
        }

        // Duyệt quy hoạch động qua các tập con
        /*
        Công thức chuyển trạng thái quy hoạch động
        Nhiệm vụ: thử mở rộng lộ trình từ trạng thái hiện tại (đã đi qua một nhóm điểm) sang thêm một điểm mới chưa từng đi
         */
        for (int mask = 1; mask < totalSubsets; mask++) { // duyệt qua từng tập địa điểm đã đi (mask)
            for (int u = 0; u < n; u++) { // giả sử điểm vừa đặt chân đến (điểm cuối) trong tập mask là u, xong mới đi tìm tiếp là từ u sẽ đến đâu
                if ((mask & (1 << u)) == 0 || Double.isInfinite(dp[mask][u])) { // == 0 -> điểm u này chưa tồn tại trong tập mask luôn, nghĩa là nó chưa có đi qua u, mà chưa đi qua u thì làm sao tìm tiếp từ u qua đâu được, còn cái isInfinite: nó vẫn là dương vô cùng và chưa có đường nào đi tới
                    continue;
                }

                for (int v = 0; v < n; v++) { // logic sang điểm mới (từ u sang v) và cập nhật kỷ lục
                    if ((mask & (1 << v)) == 0 && allowedPosition(v, Integer.bitCount(mask), positionByNode, nodeByPosition)) {
                        int nextMask = mask | (1 << v); // bật bit v: đánh dấu là đã đi thêm v
                        double newCost = dp[mask][u] + dist[u][v]; // Chi phí cũ + đường đi từ u sang v

                        if (newCost < dp[nextMask][v]) { // Nếu lộ trình mới này rẻ hơn kỷ lục cũ
                            dp[nextMask][v] = newCost;   // Cập nhật kỷ lục ngắn nhất mới
                            parent[nextMask][v] = u;     // Ghi sổ: Muốn đến v tối ưu thì phải đi từ u sang
                        }
                    }
                }
            }
        }

        /*
        Giả sử bạn đang có:Tập đã đi: {0, 1} (mask = 3), điểm cuối đang đứng là 1.
        Chi phí đã tốn: dp[3][1] = 10.
        Điểm chưa đi: 2 (dist[1][2] = 20).
        Đoạn code sẽ tính: newCost = dp[3][1] + dist[1][2] = 10 + 20 = 30
        Tập mới là {0, 1, 2} (nextMask = 7), điểm dừng chân mới là 2:
         => dp[7][2] = 30, parent[7][2] = 1
         */

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

    private boolean allowedPosition(int node, int position, int[] positionByNode, int[] nodeByPosition) {
        return (positionByNode[node] == -1 || positionByNode[node] == position)
                && (nodeByPosition[position] == -1 || nodeByPosition[position] == node);
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
            double totalCost = dp[fullMask][u] + dist[u][0]; // Chi phí đi hết tất cả các điểm và đang dừng lại ở điểm u + Chi phí đi chặng cuối cùng từ u trở về điểm xuất phát 0
            if (totalCost < minTotalCost) {
                minTotalCost = totalCost;
                bestEndNode = u;
            }
        }

        if (bestEndNode == -1) throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
        List<Integer> path = backtrackPath(bestEndNode, fullMask, parent);
        path.add(0); // Khép vòng quay lại điểm xuất phát index 0
        return path;
    }

    private List<Integer> reconstructOneWayPath(int n, int fullMask, double[][] dp, int[][] parent, Integer fixedEndIndex) {
        int bestEndNode;

        if (fixedEndIndex != null) {
            bestEndNode = fixedEndIndex; // Người dùng đã chỉ định trước điểm kết thúc
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

        if (Double.isInfinite(dp[fullMask][bestEndNode])) throw new AppException(ErrorCode.INVALID_TSP_LOCATIONS);
        return backtrackPath(bestEndNode, fullMask, parent);
    }

    private List<Integer> backtrackPath(int endNode, int fullMask, int[][] parent) {
        List<Integer> path = new ArrayList<>();
        int curr = endNode;
        int mask = fullMask;

        while (curr != -1) {
            path.add(curr);
            int prev = parent[mask][curr]; // Xem ai dẫn đến curr
            mask = mask ^ (1 << curr);     // Tắt bit của curr: loại curr khỏi tập mask
            curr = prev;                   // Lùi bước về đỉnh trước đó
        }

        Collections.reverse(path); // Đảo ngược lại để có thứ tự từ đầu đến cuối
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
                .build()) : List.of();

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
