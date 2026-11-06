package com.travel.weather.service;

import com.travel.common.exception.*;
import com.travel.weather.enums.*;
import com.travel.weather.model.*;
import com.travel.weather.repository.TripWeatherRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.travel.security.model.CustomUserDetails;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripWeatherServiceTest {
    @Mock TripWeatherRepository repository;
    @Mock WeatherService weather;
    final UUID owner=UUID.randomUUID(), id=UUID.randomUUID();
    final LocalDate today=LocalDate.of(2026,11,7);
    TripWeatherService service;
    @BeforeEach void setup() {
        service=new TripWeatherService(repository,weather,Clock.fixed(Instant.parse("2026-11-06T17:30:00Z"),ZoneOffset.UTC));
        var principal=mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(owner);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    void dates(LocalDate start,LocalDate end) {
        when(repository.trip(owner,id)).thenReturn(Optional.of(new TripWeatherRepository.TripDates(start,end,null)));
    }
    TripWeather.Location location(LocalDate date) {
        var location=new TripWeather.Location(UUID.randomUUID(),"Saved place",16,108,"SAVED_PLACE");
        when(repository.location(owner,id,null,date)).thenReturn(Optional.of(location));return location;
    }
    WeatherForecastResult forecast(LocalDate date,String provider) {
        var hour=new HourlyWeather(date.atTime(12,0),23,30,0,1,WeatherCondition.PARTLY_CLOUDY,WeatherRiskLevel.SAFE);
        return new WeatherForecastResult(16,108,"Asia/Ho_Chi_Minh",List.of(new DailyWeatherSummary(date,20,26,30,0,WeatherCondition.PARTLY_CLOUDY,WeatherRiskLevel.SAFE,List.of(hour))),provider);
    }
    @Test void missingOrForeignTripDoesNotCallProvider() {
        when(repository.trip(owner,id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id,null)).isInstanceOfSatisfying(AppException.class,e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TRIP_NOT_FOUND));
        verifyNoInteractions(weather);
    }
    @Test void noDatesDoesNotInventAForecast() {
        dates(null,null);assertThat(service.get(id,null).unavailableReason()).isEqualTo("NO_DATES");verifyNoInteractions(weather);
    }
    @Test void distantTripDoesNotSpendProviderCalls() {
        dates(today.plusDays(15),today.plusDays(20));
        var result=service.get(id,null);assertThat(result.unavailableReason()).isEqualTo("OUTSIDE_FORECAST_WINDOW");
        assertThat(result.today()).isEqualTo(today);verifyNoInteractions(weather);
    }
    @Test void pastTripIsNotForecastAsToday() {
        dates(today.minusDays(2),today.minusDays(1));
        assertThat(service.get(id,null).unavailableReason()).isEqualTo("PAST_DATE");verifyNoInteractions(weather);
    }
    @Test void missingLocationDoesNotUseAnArbitraryCity() {
        dates(today,today);when(repository.location(owner,id,null,today)).thenReturn(Optional.empty());
        assertThat(service.get(id,null).unavailableReason()).isEqualTo("NO_LOCATION");verifyNoInteractions(weather);
    }
    @Test void lastAvailableDayClipsRangeAndNeverRequestsCurrentForFuture() {
        var date=today.plusDays(14);dates(date,date.plusDays(3));location(date);
        var forecast=forecast(date,"open-meteo");when(weather.getForecast(16,108,date,date)).thenReturn(forecast);
        var result=service.get(id,null);assertThat(result.forecast()).isSameAs(forecast);assertThat(result.current()).isNull();
        assertThat(result.availableThrough()).isEqualTo(date);verify(weather,never()).getCurrentWeather(anyDouble(),anyDouble());
    }
    @Test void currentFailurePreservesTheRealHourlyForecast() {
        dates(today,today);location(today);var forecast=forecast(today,"open-meteo");
        when(weather.getForecast(16,108,today,today)).thenReturn(forecast);
        when(weather.getCurrentWeather(16,108)).thenThrow(new AppException(ErrorCode.WEATHER_UNAVAILABLE));
        var result=service.get(id,today);assertThat(result.forecast()).isSameAs(forecast);assertThat(result.current()).isNull();assertThat(result.unavailableReason()).isNull();
    }
    @Test void mockConfigurationIsExplicitlyUnavailableForTripUi() {
        dates(today,today);location(today);when(weather.getForecast(16,108,today,today)).thenReturn(forecast(today,"mock"));
        var result=service.get(id,today);assertThat(result.unavailableReason()).isEqualTo("MOCK_PROVIDER");assertThat(result.forecast()).isNull();
    }
}
