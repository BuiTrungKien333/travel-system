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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
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
            @Parameter(description = "Vĩ độ GPS [-90.0, 90.0]", example = "16.0592")
            @RequestParam("latitude")
            @DecimalMin(value = "-90.0", message = "Vĩ độ (latitude) phải nằm trong khoảng [-90.0, 90.0]")
            @DecimalMax(value = "90.0", message = "Vĩ độ (latitude) phải nằm trong khoảng [-90.0, 90.0]")
            double latitude,

            @Parameter(description = "Kinh độ GPS [-180.0, 180.0]", example = "108.2464")
            @RequestParam("longitude")
            @DecimalMin(value = "-180.0", message = "Kinh độ (longitude) phải nằm trong khoảng [-180.0, 180.0]")
            @DecimalMax(value = "180.0", message = "Kinh độ (longitude) phải nằm trong khoảng [-180.0, 180.0]")
            double longitude
    ) {
        validateCoordinates(latitude, longitude);
        log.info("[WEATHER_CONTROLLER] Fetching current weather for coordinates: ({}, {})", latitude, longitude);
        CurrentWeather currentWeather = weatherService.getCurrentWeather(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(currentWeather));
    }

    @GetMapping("/forecast")
    @Operation(summary = "Lấy dự báo thời tiết theo ngày (tùy chọn chi tiết từng giờ)",
               description = "Dự báo thời tiết cho chuyến đi theo khoảng ngày. Mặc định trả về bản tóm tắt theo ngày gọn nhẹ (~30 dòng). Truyền includeHourly=true để lấy chi tiết 24 giờ từng ngày.")
    public ResponseEntity<ApiResponse<WeatherForecastResult>> getWeatherForecast(
            @Parameter(description = "Vĩ độ GPS [-90.0, 90.0]", example = "16.0592")
            @RequestParam("latitude")
            @DecimalMin(value = "-90.0", message = "Vĩ độ (latitude) phải nằm trong khoảng [-90.0, 90.0]")
            @DecimalMax(value = "90.0", message = "Vĩ độ (latitude) phải nằm trong khoảng [-90.0, 90.0]")
            double latitude,

            @Parameter(description = "Kinh độ GPS [-180.0, 180.0]", example = "108.2464")
            @RequestParam("longitude")
            @DecimalMin(value = "-180.0", message = "Kinh độ (longitude) phải nằm trong khoảng [-180.0, 180.0]")
            @DecimalMax(value = "180.0", message = "Kinh độ (longitude) phải nằm trong khoảng [-180.0, 180.0]")
            double longitude,
            @Parameter(description = "Ngày bắt đầu (mặc định hôm nay)", example = "2026-10-01")
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Ngày kết thúc (mặc định bằng startDate)", example = "2026-10-03")
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Có bao gồm chi tiết thời tiết 24 giờ từng ngày không (mặc định false)", example = "false")
            @RequestParam(value = "includeHourly", required = false, defaultValue = "false")
            boolean includeHourly
    ) {
        validateCoordinates(latitude, longitude);
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start;

        log.info("[WEATHER_CONTROLLER] Fetching weather forecast for coordinates: ({}, {}) from {} to {}, includeHourly={}",
                latitude, longitude, start, end, includeHourly);
        WeatherForecastResult forecast = weatherService.getForecast(latitude, longitude, start, end);
        WeatherForecastResult responseData = includeHourly ? forecast : forecast.withoutHourlyDetails();
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(String.format("Vĩ độ (latitude) phải nằm trong khoảng [-90.0, 90.0], nhận giá trị: %s", latitude));
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(String.format("Kinh độ (longitude) phải nằm trong khoảng [-180.0, 180.0], nhận giá trị: %s", longitude));
        }
    }
}
