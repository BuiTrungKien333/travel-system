package com.travel.weather.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Schema(description = "Kết quả dự báo thời tiết cho một vị trí địa lý")
public record WeatherForecastResult(
        double latitude,
        double longitude,
        String timezone,
        List<DailyWeatherSummary> dailySummaries,
        String providerName
) {
    public WeatherForecastResult {
        dailySummaries = dailySummaries != null ? List.copyOf(dailySummaries) : List.of();
    }

    /**
     * Lấy dự báo thời tiết theo ngày cụ thể
     */
    public Optional<DailyWeatherSummary> getDailySummaryAt(LocalDate date) {
        return dailySummaries.stream()
                .filter(d -> d.date().equals(date))
                .findFirst();
    }

    /**
     * Lấy dự báo thời tiết tại khung giờ gần nhất với thời điểm yêu cầu
     */
    public Optional<HourlyWeather> getForecastAt(LocalDateTime dateTime) {
        if (dateTime == null) return Optional.empty();

        LocalDate date = dateTime.toLocalDate();
        Optional<DailyWeatherSummary> dailyOpt = getDailySummaryAt(date);
        if (dailyOpt.isEmpty()) {
            return Optional.empty();
        }

        int targetHour = dateTime.getHour();
        return dailyOpt.get().hourlyForecasts().stream()
                .filter(h -> h.time().getHour() == targetHour)
                .findFirst();
    }
}
