package com.travel.weather.provider;

import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.WeatherForecastResult;

import java.time.LocalDate;

/**
 * Interface hợp đồng chung cho mọi Weather Provider (Open-Meteo, OpenWeatherMap, Mock...)
 */
public interface WeatherProvider {

    /**
     * Lấy dự báo thời tiết theo tọa độ GPS và khoảng ngày
     *
     * @param latitude Vĩ độ
     * @param longitude Kinh độ
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return Kết quả dự báo chi tiết theo ngày và giờ
     */
    WeatherForecastResult fetchForecast(double latitude, double longitude, LocalDate startDate, LocalDate endDate);

    /**
     * Lấy thông tin thời tiết thực tế tại tọa độ và thời điểm hiện tại
     *
     * @param latitude Vĩ độ
     * @param longitude Kinh độ
     * @return Thông tin thời tiết hiện tại (nhiệt độ, độ ẩm, gió, mưa, rủi ro)
     */
    CurrentWeather fetchCurrentWeather(double latitude, double longitude);

    /**
     * Kiểm tra xem provider có sẵn sàng hoạt động hay không
     */
    boolean isAvailable();

    /**
     * Tên định danh của provider (ví dụ: "open-meteo", "mock", "openweathermap")
     */
    String getProviderName();
}
