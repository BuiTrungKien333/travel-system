package com.travel.weather.controller;

import com.travel.common.response.ApiResponse;
import com.travel.weather.model.TripWeather;
import com.travel.weather.service.TripWeatherService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trips/{tripId}/weather")
public class TripWeatherController {

    private final TripWeatherService service;

    @GetMapping
    public ApiResponse<TripWeather> get(
        @PathVariable UUID tripId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(service.get(tripId, date));
    }
}
