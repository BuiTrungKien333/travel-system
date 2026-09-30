package com.travel.weather.controller;

import com.travel.common.response.ApiResponse;
import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.WeatherForecastResult;
import com.travel.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/public/weather")
@RequiredArgsConstructor
@Slf4j(topic = "WEATHER_CONTROLLER")
@Tag(name = "Weather", description = "API truy vấn thời tiết thực tế và dự báo cho lập kế hoạch du lịch")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/current")
    @Operation(summary = "Lấy thời tiết thực tế tại tọa độ và thời điểm hiện tại",
               description = "Trả về nhiệt độ hiện tại, cảm nhận thực tế, độ ẩm, tốc độ gió, khả năng mưa và đánh giá mức độ rủi ro ngoài trời")
    public ResponseEntity<ApiResponse<CurrentWeather>> getCurrentWeather(
            @Parameter(description = "Vĩ độ GPS", example = "16.0592")
            @RequestParam("latitude") double latitude,
            @Parameter(description = "Kinh độ GPS", example = "108.2464")
            @RequestParam("longitude") double longitude
    ) {
        log.info("[WEATHER_CONTROLLER] Fetching current weather for coordinates: ({}, {})", latitude, longitude);
        CurrentWeather currentWeather = weatherService.getCurrentWeather(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(currentWeather));
    }

    @GetMapping("/forecast")
    @Operation(summary = "Lấy dự báo thời tiết chi tiết theo ngày và giờ",
               description = "Dự báo thời tiết cho chuyến đi theo khoảng ngày, bao gồm nhiệt độ, khả năng mưa, lượng mưa từng giờ")
    public ResponseEntity<ApiResponse<WeatherForecastResult>> getWeatherForecast(
            @Parameter(description = "Vĩ độ GPS", example = "16.0592")
            @RequestParam("latitude") double latitude,
            @Parameter(description = "Kinh độ GPS", example = "108.2464")
            @RequestParam("longitude") double longitude,
            @Parameter(description = "Ngày bắt đầu (mặc định hôm nay)", example = "2026-10-01")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (mặc định bằng startDate)", example = "2026-10-03")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start;

        log.info("[WEATHER_CONTROLLER] Fetching weather forecast for coordinates: ({}, {}) from {} to {}",
                latitude, longitude, start, end);
        WeatherForecastResult forecast = weatherService.getForecast(latitude, longitude, start, end);
        return ResponseEntity.ok(ApiResponse.success(forecast));
    }
}
