package com.travel.weather.service;

import com.travel.weather.model.HourlyWeather;
import com.travel.weather.model.WeatherForecastResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface WeatherService {

    /**
     * Lấy dự báo thời tiết tổng thể cho tọa độ trong khoảng ngày
     */
    WeatherForecastResult getForecast(double latitude, double longitude, LocalDate startDate, LocalDate endDate);

    /**
     * Lấy dự báo thời tiết chi tiết tại một thời điểm cụ thể (ngày và giờ)
     */
    Optional<HourlyWeather> getForecastAt(double latitude, double longitude, LocalDateTime dateTime);

    /**
     * Kiểm tra xem tại thời điểm và tọa độ đó có nguy cơ thời tiết xấu đối với hoạt động ngoài trời không
     */
    boolean isOutdoorHazardous(double latitude, double longitude, LocalDateTime dateTime);
}
