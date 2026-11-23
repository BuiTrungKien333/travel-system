package com.travel.weather.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.model.WeatherForecastResult;
import com.travel.weather.provider.WeatherProvider;
import com.travel.weather.provider.WeatherProviderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final WeatherProviderFactory providerFactory;

    @Override
    public WeatherForecastResult getForecast(double latitude, double longitude, LocalDate startDate, LocalDate endDate) {
        validateCoordinates(latitude, longitude);
        if (startDate == null || endDate == null || endDate.isBefore(startDate)
                || java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) > 15)
            throw new AppException(ErrorCode.VALIDATION_ERROR);
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
    public CurrentWeather getCurrentWeather(double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        WeatherProvider provider = providerFactory.getProvider();
        return provider.fetchCurrentWeather(latitude, longitude);
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180)
            throw new AppException(ErrorCode.VALIDATION_ERROR);
    }
}
