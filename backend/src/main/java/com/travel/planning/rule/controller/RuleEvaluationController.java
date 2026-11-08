package com.travel.planning.rule.controller;

import com.travel.common.response.ApiResponse;
import com.travel.planning.rule.dto.EvaluateItineraryRequest;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.service.RuleEvaluatorService;
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
@RequestMapping({"/api/v1/public/planning/rules", "/api/v1/planning/rules"})
@RequiredArgsConstructor
@Slf4j(topic = "RULE_EVALUATION_CONTROLLER")
@Tag(name = "Planning - Rule Evaluator", description = "API thẩm định các quy tắc xác định trong lịch trình trước khi gọi AI")
public class RuleEvaluationController {

    private final RuleEvaluatorService ruleEvaluatorService;

    @PostMapping("/evaluate")
    @Operation(summary = "Thẩm định các quy tắc xác định trong lịch trình",
               description = "Kiểm tra xung đột giờ mở/đóng cửa, thời tiết khắc nghiệt ngoài trời và tính khả thi của thời gian di chuyển. Chỉ bật cờ shouldTriggerAi khi phát hiện xung đột.")
    public ResponseEntity<ApiResponse<ItineraryEvaluationResult>> evaluateItinerary(
            @Valid @RequestBody EvaluateItineraryRequest request
    ) {
        log.info("[RULE_EVALUATION_CONTROLLER] Received itinerary evaluation request with {} locations",
                request.items().size());

        ItineraryEvaluationResult result = ruleEvaluatorService.evaluateItinerary(request.items());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
