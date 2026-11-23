package com.travel.weather.service;

import com.travel.weather.model.CurrentWeather;
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
     * Lấy thông tin thời tiết thực tế tại tọa độ và thời điểm hiện tại
     *
     * @param latitude Vĩ độ
     * @param longitude Kinh độ
     * @return Dữ liệu thời tiết hiện tại (nhiệt độ, cảm nhận, độ ẩm, mưa, gió, điều kiện và rủi ro)
     */
    CurrentWeather getCurrentWeather(double latitude, double longitude);
}
