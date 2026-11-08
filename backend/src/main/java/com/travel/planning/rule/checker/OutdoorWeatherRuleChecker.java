package com.travel.planning.rule.checker;

import com.travel.place.enums.PlaceType;
import com.travel.planning.rule.enums.IssueCategory;
import com.travel.planning.rule.enums.IssueSeverity;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "OUTDOOR_WEATHER_RULE")
public class OutdoorWeatherRuleChecker implements ScheduleRuleChecker {

    private final WeatherService weatherService;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    @Override
    public List<DetectedIssue> evaluate(List<ScheduleItemContext> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<DetectedIssue> issues = new ArrayList<>();

        for (ScheduleItemContext item : items) {
            // Chỉ kiểm tra các địa điểm ngoài trời (OUTDOOR) hoặc hỗn hợp (HYBRID)
            if (item.placeType() != PlaceType.OUTDOOR && item.placeType() != PlaceType.HYBRID) {
                continue;
            }

            if (item.arrivalTime() == null) {
                continue;
            }

            Optional<HourlyWeather> weatherOpt = weatherService.getForecastAt(
                    item.latitude(), item.longitude(), item.arrivalTime());

            if (weatherOpt.isEmpty()) {
                continue;
            }

            HourlyWeather weather = weatherOpt.get();
            String formattedTime = item.arrivalTime().format(TIME_FORMAT);

            // Ca 1: Thời tiết cực đoan / Nguy hiểm (Dông bão, mưa lớn, nhiệt độ >= 38°C)
            if (weather.riskLevel() == WeatherRiskLevel.HAZARDOUS || weather.condition().isSevere() || weather.temperature() >= 38.0) {
                String reason;
                if (weather.condition() == WeatherCondition.THUNDERSTORM) {
                    reason = String.format("Dự báo có DÔNG BÃO, sấm chớp nguy hiểm lúc %s tại '%s'. Các hoạt động ngoài trời (leo núi, tắm biển, đi đèo) tiềm ẩn rủi ro tai nạn cao.",
                            formattedTime, item.placeName());
                } else if (weather.condition() == WeatherCondition.HEAVY_RAIN) {
                    reason = String.format("Dự báo có MƯA LỚN DIỆN RỘNG (lượng mưa %.1f mm, xác suất %d%%) lúc %s tại '%s', không thể tham quan ngoài trời.",
                            weather.rainMm(), weather.precipitationProbability(), formattedTime, item.placeName());
                } else {
                    reason = String.format("Nhiệt độ ngoài trời cực đoan lên tới %.1f°C lúc %s tại '%s', nguy cơ sốc nhiệt cao đối với hoạt động ngoài trời.",
                            weather.temperature(), formattedTime, item.placeName());
                }

                issues.add(new DetectedIssue(
                        item.placeId(),
                        item.placeName(),
                        item.arrivalTime(),
                        IssueCategory.HAZARDOUS_WEATHER,
                        IssueSeverity.CRITICAL,
                        reason,
                        "Hủy hoặc tạm hoãn hoạt động ngoài trời này, chuyển sang địa điểm trong nhà (INDOOR) như bảo tàng, quán cafe, trung tâm thương mại hoặc đổi thời gian.",
                        Map.of(
                                "temperature", weather.temperature(),
                                "condition", weather.condition().name(),
                                "rainProbability", weather.precipitationProbability(),
                                "rainMm", weather.rainMm()
                        )
                ));
            }
            // Ca 2: Mưa rào, mưa vừa, khả năng mưa cao
            else if (weather.condition().isRainyOrStormy() || weather.precipitationProbability() >= 60 || weather.rainMm() > 0.5) {
                issues.add(new DetectedIssue(
                        item.placeId(),
                        item.placeName(),
                        item.arrivalTime(),
                        IssueCategory.ADVERSE_WEATHER,
                        IssueSeverity.IMPORTANT,
                        String.format("Dự báo có mưa (%s, lượng mưa %.1f mm, xác suất mưa %d%%) lúc %s tại '%s'.",
                                weather.condition().getDescription(), weather.rainMm(), weather.precipitationProbability(), formattedTime, item.placeName()),
                        "Cần mang theo ô/dù, áo mưa bỏ túi, bọc chống nước cho đồ điện tử hoặc chuẩn bị phương án vào khu vực có mái che.",
                        Map.of(
                                "temperature", weather.temperature(),
                                "condition", weather.condition().name(),
                                "rainProbability", weather.precipitationProbability(),
                                "rainMm", weather.rainMm()
                        )
                ));
            }
            // Ca 3: Nắng nóng giữa trưa (11:00 - 14:00) với nhiệt độ >= 35°C
            else if (weather.temperature() >= 35.0 && item.arrivalTime().getHour() >= 11 && item.arrivalTime().getHour() <= 14) {
                issues.add(new DetectedIssue(
                        item.placeId(),
                        item.placeName(),
                        item.arrivalTime(),
                        IssueCategory.ADVERSE_WEATHER,
                        IssueSeverity.IMPORTANT,
                        String.format("Trời nắng gắt đỉnh điểm (nhiệt độ %.1f°C) vào khung giờ trưa lúc %s tại '%s'.",
                                weather.temperature(), formattedTime, item.placeName()),
                        "Nên trang bị kem chống nắng, mũ nón rộng vành, kính râm, uống đủ nước hoặc sắp xếp thời gian ăn trưa, nghỉ ngơi trong bóng râm.",
                        Map.of(
                                "temperature", weather.temperature(),
                                "condition", weather.condition().name()
                        )
                ));
            }
        }

        log.debug("[OUTDOOR_WEATHER_RULE] Evaluated {} items, found {} weather issues", items.size(), issues.size());
        return issues;
    }

    @Override
    public String getRuleName() {
        return "OutdoorWeatherRule";
    }
}
