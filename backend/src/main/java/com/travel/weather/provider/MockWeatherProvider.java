package com.travel.weather.provider;

import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.model.WeatherForecastResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component("mockWeatherProvider")
@Slf4j
public class MockWeatherProvider implements WeatherProvider {

    @Override
    public WeatherForecastResult fetchForecast(double latitude, double longitude, LocalDate startDate, LocalDate endDate) {
        log.info("[MOCK_WEATHER] Generating mock weather forecast for ({}, {}) from {} to {}",
                latitude, longitude, startDate, endDate);

        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start;
        if (end.isBefore(start)) {
            end = start;
        }

        List<DailyWeatherSummary> dailySummaries = new ArrayList<>();
        LocalDate current = start;

        while (!current.isAfter(end)) {
            List<HourlyWeather> hourly = new ArrayList<>();
            for (int hour = 0; hour < 24; hour++) {
                LocalDateTime time = current.atTime(hour, 0);
                double temp = 26.0 + Math.sin((hour - 6) * Math.PI / 12) * 6.0; // 20°C - 32°C
                int rainProb = (hour >= 13 && hour <= 15) ? 40 : 10;
                double rainMm = (hour >= 13 && hour <= 15) ? 1.5 : 0.0;
                WeatherCondition condition = (rainProb > 30) ? WeatherCondition.LIGHT_RAIN : WeatherCondition.PARTLY_CLOUDY;
                WeatherRiskLevel risk = WeatherRiskLevel.evaluate(condition, temp, rainProb);

                hourly.add(new HourlyWeather(time, Math.round(temp * 10.0) / 10.0, rainProb, rainMm, condition == WeatherCondition.LIGHT_RAIN ? 51 : 1, condition, risk));
            }

            dailySummaries.add(new DailyWeatherSummary(
                    current,
                    23.0,
                    32.0,
                    40,
                    1.5,
                    WeatherCondition.PARTLY_CLOUDY,
                    WeatherRiskLevel.SAFE,
                    hourly
            ));

            current = current.plusDays(1);
        }

        return new WeatherForecastResult(
                latitude,
                longitude,
                "Asia/Ho_Chi_Minh",
                dailySummaries,
                "mock"
        );
    }

    @Override
    public CurrentWeather fetchCurrentWeather(double latitude, double longitude) {
        log.info("[MOCK_WEATHER] Generating mock current weather for ({}, {})", latitude, longitude);
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        boolean isDay = hour >= 6 && hour <= 18;
        double temp = 28.5;
        double apparentTemp = 32.0;
        int humidity = 75;
        double rainMm = 0.0;
        double windSpeed = 12.0;
        int code = 1;
        WeatherCondition condition = WeatherCondition.PARTLY_CLOUDY;
        WeatherRiskLevel risk = WeatherRiskLevel.SAFE;

        return new CurrentWeather(
                latitude,
                longitude,
                now,
                temp,
                apparentTemp,
                humidity,
                rainMm,
                windSpeed,
                isDay,
                code,
                condition,
                risk,
                null
        );
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
