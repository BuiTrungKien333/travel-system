package com.travel.planning.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.dto.request.ApplyRecommendationRequest;
import com.travel.planning.dto.request.OptimizeAndAuditRequest;
import com.travel.planning.dto.response.TripOptimizationResponse;
import com.travel.planning.service.TripOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/public/planning/itinerary", "/api/v1/planning/itinerary"})
@RequiredArgsConstructor
@Slf4j(topic = "ITINERARY_OPTIMIZATION_CONTROLLER")
@Tag(name = "Planning - Itinerary Optimization & AI Copilot",
     description = "API tối ưu lộ trình toàn diện (Held-Karp + Weather + Rule Evaluator + Gemini AI Copilot HITL)")
public class ItineraryOptimizationController {

    private final TripOptimizationService tripOptimizationService;

    @PostMapping("/optimize-and-audit")
    @Operation(summary = "Tối ưu hóa thứ tự lộ trình và thẩm định toàn diện bằng AI Copilot (HITL)",
               description = "Áp dụng thuật toán Held-Karp, đồng bộ dự báo thời tiết và chạy bộ thẩm định quy tắc xác định. Nếu lịch trình hoàn hảo, AI im lặng (0 token tiêu tốn); nếu phát hiện xung đột, Gemini Copilot sẽ đề xuất tối đa 3 thẻ hành động.")
    public ResponseEntity<ApiResponse<TripOptimizationResponse>> optimizeAndAuditItinerary(
            @Valid @RequestBody OptimizeAndAuditRequest request
    ) {
        log.info("[ITINERARY_CONTROLLER] Received optimize-and-audit request for destination='{}', {} locations",
                request.destination(), request.locations().size());

        TripOptimizationResponse response = tripOptimizationService.optimizeAndAuditItinerary(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/apply-recommendation")
    @Operation(summary = "Thực thi 1-click hành động đề xuất từ AI Copilot (Human-In-The-Loop)",
               description = "Thực thi hành động người dùng đã chọn (hoán đổi địa điểm, chỉnh giờ...) và tự động tái thẩm định lại lịch trình mới.")
    public ResponseEntity<ApiResponse<TripOptimizationResponse>> applyRecommendation(
            @Valid @RequestBody ApplyRecommendationRequest request
    ) {
        log.info("[ITINERARY_CONTROLLER] Received apply-recommendation request: actionType={}, cardId={}",
                request.actionType(), request.cardId());

        TripOptimizationResponse response = tripOptimizationService.applyCopilotRecommendation(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
