package com.travel.weather.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travel.weather.config.WeatherConfigProperties;
import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.model.WeatherForecastResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component("openMeteoWeatherProvider")
@Slf4j
public class OpenMeteoWeatherProvider implements WeatherProvider {

    private final WeatherConfigProperties weatherConfig;
    private final MockWeatherProvider fallbackProvider;
    private final RestClient restClient;

    public OpenMeteoWeatherProvider(WeatherConfigProperties weatherConfig, MockWeatherProvider fallbackProvider) {
        this.weatherConfig = weatherConfig;
        this.fallbackProvider = fallbackProvider;

        int timeoutSeconds = weatherConfig.getOpenMeteo().getTimeoutSeconds();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 10));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    // Constructor dành cho Unit Test để mock RestClient
    public OpenMeteoWeatherProvider(WeatherConfigProperties weatherConfig, MockWeatherProvider fallbackProvider, RestClient restClient) {
        this.weatherConfig = weatherConfig;
        this.fallbackProvider = fallbackProvider;
        this.restClient = restClient;
    }

    @Override
    public WeatherForecastResult fetchForecast(double latitude, double longitude, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start;
        if (end.isBefore(start)) {
            end = start;
        }

        String baseUrl = weatherConfig.getOpenMeteo().getBaseUrl();
        String timezone = weatherConfig.getOpenMeteo().getDefaultTimezone();

        String uri = UriComponentsBuilder.fromUriString(baseUrl + "/forecast")
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("hourly", "temperature_2m,precipitation_probability,rain,weather_code")
                .queryParam("start_date", start.toString())
                .queryParam("end_date", end.toString())
                .queryParam("timezone", timezone)
                .build()
                .toUriString();

        log.info("[OPEN_METEO] Requesting weather forecast for ({}, {}) from {} to {}", latitude, longitude, start, end);

        try {
            OpenMeteoApiResponse response = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OpenMeteoApiResponse.class);

            if (response == null || response.hourly() == null || response.hourly().time() == null) {
                log.warn("[OPEN_METEO] Null or empty response received. Using mock fallback.");
                return fallbackProvider.fetchForecast(latitude, longitude, start, end);
            }

            return mapToForecastResult(latitude, longitude, response, start, end);

        } catch (Exception ex) {
            log.error("[OPEN_METEO] Failed to call Open-Meteo API: {}. Activating mock fallback.", ex.getMessage());
            return fallbackProvider.fetchForecast(latitude, longitude, start, end);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "open-meteo";
    }

    private WeatherForecastResult mapToForecastResult(
            double latitude,
            double longitude,
            OpenMeteoApiResponse response,
            LocalDate startDate,
            LocalDate endDate
    ) {
        OpenMeteoHourlyDto hourlyDto = response.hourly();
        List<String> times = hourlyDto.time();
        List<Double> temps = hourlyDto.temperature2m();
        List<Integer> rainProbs = hourlyDto.precipitationProbability();
        List<Double> rains = hourlyDto.rain();
        List<Integer> weatherCodes = hourlyDto.weatherCode();

        int size = times != null ? times.size() : 0;
        List<HourlyWeather> allHourly = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            LocalDateTime time = LocalDateTime.parse(times.get(i), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            double temp = (temps != null && i < temps.size() && temps.get(i) != null) ? temps.get(i) : 25.0;
            int rainProb = (rainProbs != null && i < rainProbs.size() && rainProbs.get(i) != null) ? rainProbs.get(i) : 0;
            double rain = (rains != null && i < rains.size() && rains.get(i) != null) ? rains.get(i) : 0.0;
            int code = (weatherCodes != null && i < weatherCodes.size() && weatherCodes.get(i) != null) ? weatherCodes.get(i) : 0;

            WeatherCondition condition = WeatherCondition.fromWmoCode(code);
            WeatherRiskLevel risk = WeatherRiskLevel.evaluate(condition, temp, rainProb);

            allHourly.add(new HourlyWeather(time, temp, rainProb, rain, code, condition, risk));
        }

        // Nhóm các giờ theo từng ngày (LocalDate)
        Map<LocalDate, List<HourlyWeather>> groupedByDate = allHourly.stream()
                .collect(Collectors.groupingBy(h -> h.time().toLocalDate(), LinkedHashMap::new, Collectors.toList()));

        List<DailyWeatherSummary> dailySummaries = new ArrayList<>();
        for (Map.Entry<LocalDate, List<HourlyWeather>> entry : groupedByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<HourlyWeather> hours = entry.getValue();

            double minTemp = hours.stream().mapToDouble(HourlyWeather::temperature).min().orElse(20.0);
            double maxTemp = hours.stream().mapToDouble(HourlyWeather::temperature).max().orElse(30.0);
            int maxRainProb = hours.stream().mapToInt(HourlyWeather::precipitationProbability).max().orElse(0);
            double totalRain = hours.stream().mapToDouble(HourlyWeather::rainMm).sum();

            // Tìm điều kiện thời tiết chủ đạo (xuất hiện nhiều nhất)
            WeatherCondition dominant = hours.stream()
                    .collect(Collectors.groupingBy(HourlyWeather::condition, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(WeatherCondition.CLEAR_SKY);

            // Tìm mức độ rủi ro cao nhất trong ngày
            WeatherRiskLevel highestRisk = hours.stream()
                    .map(HourlyWeather::riskLevel)
                    .max(Comparator.comparingInt(Enum::ordinal))
                    .orElse(WeatherRiskLevel.SAFE);

            dailySummaries.add(new DailyWeatherSummary(
                    date,
                    roundOneDecimal(minTemp),
                    roundOneDecimal(maxTemp),
                    maxRainProb,
                    roundOneDecimal(totalRain),
                    dominant,
                    highestRisk,
                    hours
            ));
        }

        return new WeatherForecastResult(
                latitude,
                longitude,
                response.timezone() != null ? response.timezone() : "Asia/Ho_Chi_Minh",
                dailySummaries,
                "open-meteo"
        );
    }

    private double roundOneDecimal(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoApiResponse(
            Double latitude,
            Double longitude,
            String timezone,
            OpenMeteoHourlyDto hourly
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoHourlyDto(
            List<String> time,
            @JsonProperty("temperature_2m")
            List<Double> temperature2m,
            @JsonProperty("precipitation_probability")
            List<Integer> precipitationProbability,
            List<Double> rain,
            @JsonProperty("weather_code")
            List<Integer> weatherCode
    ) {
    }
}
