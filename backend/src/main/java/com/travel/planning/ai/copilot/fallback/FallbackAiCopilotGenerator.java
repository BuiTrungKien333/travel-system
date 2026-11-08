package com.travel.planning.ai.copilot.fallback;

import com.travel.planning.ai.copilot.enums.CopilotActionType;
import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;
import com.travel.planning.ai.copilot.model.AiRecommendationCard;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.model.DetectedIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j(topic = "FALLBACK_AI_COPILOT_GENERATOR")
public class FallbackAiCopilotGenerator {

    public AiCopilotAnalysisResult generateFallback(AiCopilotPromptContext context) {
        log.info("[FALLBACK_COPILOT] Generating rule-based caring analysis for destination: {}",
                context.destination());

        String greeting = buildCaringGreeting(context);
        List<AiRecommendationCard> cards = new ArrayList<>();

        List<DetectedIssue> issues = context.detectedIssues();
        int maxCards = Math.min(3, issues.size());

        for (int i = 0; i < maxCards; i++) {
            DetectedIssue issue = issues.get(i);
            cards.add(mapIssueToCard(issue, context));
        }

        return new AiCopilotAnalysisResult(
                greeting,
                cards,
                "rule-based-fallback"
        );
    }

    private String buildCaringGreeting(AiCopilotPromptContext context) {
        String weather = context.weatherOverview().toLowerCase();
        if (weather.contains("mưa") || weather.contains("rain")) {
            return String.format("Chào bạn! Hôm nay tại %s dự báo có mưa. Bạn nhớ mang theo áo mưa bỏ túi hoặc ô dù để chuyến đi luôn thuận lợi nhé!",
                    context.destination() != null ? context.destination() : "điểm đến");
        } else if (weather.contains("nắng") || weather.contains("nhiệt độ cao")) {
            return String.format("Chào bạn! Thời tiết tại %s hôm nay khá nắng đẹp. Hãy chuẩn bị sẵn mũ nón, kem chống nắng và bổ sung đủ nước khi tham quan nhé!",
                    context.destination() != null ? context.destination() : "điểm đến");
        } else {
            return String.format("Chào bạn! Chúc bạn có một ngày khám phá tuyệt vời tại %s. Hãy cùng rà soát lại một vài lưu ý nhỏ dưới đây nhé!",
                    context.destination() != null ? context.destination() : "điểm đến");
        }
    }

    private AiRecommendationCard mapIssueToCard(DetectedIssue issue, AiCopilotPromptContext context) {
        String cardId = UUID.randomUUID().toString();
        CopilotActionType actionType;
        String actionButtonText;
        Map<String, Object> payload;

        if (issue.category() == IssueCategory.CLOSING_TIME_CONFLICT) {
            actionType = CopilotActionType.SWAP_PLACES;
            actionButtonText = "Đảo thứ tự tham quan";
            payload = Map.of(
                    "targetPlaceId", issue.placeId() != null ? issue.placeId().toString() : "",
                    "targetPlaceName", issue.placeName()
            );
        } else if (issue.category() == IssueCategory.HAZARDOUS_WEATHER) {
            actionType = CopilotActionType.REPLACE_PLACE;
            actionButtonText = "Đổi sang điểm trong nhà";
            payload = Map.of(
                    "targetPlaceId", issue.placeId() != null ? issue.placeId().toString() : "",
                    "suggestedType", "INDOOR"
            );
        } else if (issue.category() == IssueCategory.TIGHT_TRANSIT_TIME) {
            actionType = CopilotActionType.CHANGE_TIME;
            actionButtonText = "Kéo giãn khoảng đệm";
            payload = Map.of(
                    "targetPlaceId", issue.placeId() != null ? issue.placeId().toString() : "",
                    "bufferAddMinutes", 15
            );
        } else {
            actionType = CopilotActionType.DISMISS;
            actionButtonText = "Đã ghi nhận";
            payload = Map.of();
        }

        return new AiRecommendationCard(
                cardId,
                issue.severity(),
                String.format("Lưu ý tại %s", issue.placeName()),
                issue.reason(),
                actionType,
                actionButtonText,
                payload
        );
    }
}
