package com.travel.weather.service;

import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.model.WeatherForecastResult;
import com.travel.weather.provider.WeatherProvider;
import com.travel.weather.provider.WeatherProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final WeatherProviderFactory providerFactory;

    @Override
    public WeatherForecastResult getForecast(double latitude, double longitude, LocalDate startDate, LocalDate endDate) {
        WeatherProvider provider = providerFactory.getProvider();
        return provider.fetchForecast(latitude, longitude, startDate, endDate);
    }

    @Override
    public Optional<HourlyWeather> getForecastAt(double latitude, double longitude, LocalDateTime dateTime) {
        if (dateTime == null) {
            return Optional.empty();
        }
        LocalDate date = dateTime.toLocalDate();
        WeatherForecastResult result = getForecast(latitude, longitude, date, date);
        return result.getForecastAt(dateTime);
    }

    @Override
    public boolean isOutdoorHazardous(double latitude, double longitude, LocalDateTime dateTime) {
        Optional<HourlyWeather> hourlyOpt = getForecastAt(latitude, longitude, dateTime);
        if (hourlyOpt.isEmpty()) {
            return false;
        }
        HourlyWeather hourly = hourlyOpt.get();
        return hourly.riskLevel() == WeatherRiskLevel.HAZARDOUS;
    }

    @Override
    public CurrentWeather getCurrentWeather(double latitude, double longitude) {
        WeatherProvider provider = providerFactory.getProvider();
        return provider.fetchCurrentWeather(latitude, longitude);
    }
}
