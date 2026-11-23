package com.travel.planning.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.dto.request.OptimizeRouteRequest;
import com.travel.planning.dto.response.TspOptimizeResponse;
import com.travel.planning.service.TspOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/public/planning/tsp", "/api/v1/planning/tsp"})
@RequiredArgsConstructor
@Tag(name = "Planning - TSP Optimization", description = "API tối ưu hóa lộ trình di chuyển du lịch theo thuật toán Held-Karp")
public class TspOptimizationController {
    private final TspOptimizationService service;

    @PostMapping("/optimize")
    @Operation(summary = "Tối ưu hóa thứ tự các địa điểm du lịch bằng thuật toán Held-Karp")
    public ResponseEntity<ApiResponse<TspOptimizeResponse>> optimizeRoute(
            @Valid @RequestBody OptimizeRouteRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Tối ưu hóa lộ trình thành công", service.optimize(request)));
    }
}
