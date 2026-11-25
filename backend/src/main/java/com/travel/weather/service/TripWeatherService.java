package com.travel.weather.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.util.SecurityUtils;
import com.travel.trip.service.TripCalendar;
import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.TripWeather;
import com.travel.weather.repository.TripWeatherRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Database reads finish before external HTTP; no DB transaction held during provider calls. */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripWeatherService {

    private final TripWeatherRepository repository;
    private final WeatherService weather;
    private final Clock clock;

    public TripWeather get(UUID id, LocalDate requested) {
        var owner = SecurityUtils.getCurrentUserId();
        var trip = repository.trip(owner, id).orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        var now = clock.instant();
        var today = TripCalendar.today(Clock.fixed(now, TripCalendar.ZONE));
        // Explicit-date provider requests follow the UTC window even in Vietnam time.
        var through = today.plusDays(14);
        var date =
            requested != null
                ? requested
                : trip.start() != null && trip.start().isAfter(today)
                  ? trip.start()
                  : trip.end() != null && trip.end().isBefore(today)
                    ? trip.end()
                    : today;
        String reason = null;
        if (TripCalendar.days(trip.start(), trip.end()) == 0) reason = "NO_DATES";
        else if (date.isBefore(trip.start()) || date.isAfter(trip.end())) throw new AppException(
            ErrorCode.VALIDATION_ERROR
        );
        else if (date.isBefore(today)) reason = "PAST_DATE";
        else if (date.isAfter(through)) reason = "OUTSIDE_FORECAST_WINDOW";
        if (reason != null) return new TripWeather(date, today, through, now, null, null, null, reason);
        var location = repository.location(owner, id, trip.provinceId(), date).orElse(null);
        if (location == null) return new TripWeather(date, today, through, now, null, null, null, "NO_LOCATION");
        var end = date;
        if (requested == null) end = trip.end().isBefore(through) ? trip.end() : through;
        var forecast = weather.getForecast(location.latitude(), location.longitude(), date, end);
        if (!"open-meteo".equals(forecast.providerName())) return new TripWeather(
            date,
            today,
            through,
            now,
            location,
            null,
            null,
            "MOCK_PROVIDER"
        );
        CurrentWeather current = null;
        if (date.equals(today)) {
            try {
                current = weather.getCurrentWeather(location.latitude(), location.longitude());
            } catch (AppException error) {
                if (
                    error.getErrorCode() != ErrorCode.WEATHER_UNAVAILABLE &&
                    error.getErrorCode() != ErrorCode.WEATHER_INVALID_RESPONSE
                ) throw error;
                log.warn(
                    "Current weather unavailable; returning the successful hourly forecast ({})",
                    error.getErrorCode()
                );
            }
        }
        return new TripWeather(date, today, through, now, location, forecast, current, null);
    }
}
