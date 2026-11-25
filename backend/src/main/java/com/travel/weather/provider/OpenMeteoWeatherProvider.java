package com.travel.weather.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.weather.config.WeatherConfigProperties;
import com.travel.weather.enums.WeatherCondition;
import com.travel.weather.enums.WeatherRiskLevel;
import com.travel.weather.model.CurrentWeather;
import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.HourlyWeather;
import com.travel.weather.model.WeatherForecastResult;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component("openMeteoWeatherProvider")
@Slf4j
public class OpenMeteoWeatherProvider implements WeatherProvider {

    private final WeatherConfigProperties weatherConfig;
    private final ObjectMapper json;
    private final RestClient restClient;

    @Autowired
    public OpenMeteoWeatherProvider(WeatherConfigProperties weatherConfig, ObjectMapper json) {
        this.weatherConfig = weatherConfig;
        this.json = json;

        int timeoutSeconds = weatherConfig.getOpenMeteo().getTimeoutSeconds();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds > 0 ? timeoutSeconds : 10));

        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // Constructor dành cho Unit Test để mock RestClient
    public OpenMeteoWeatherProvider(WeatherConfigProperties weatherConfig, RestClient restClient, ObjectMapper json) {
        this.weatherConfig = weatherConfig;
        this.json = json;
        this.restClient = restClient;
    }

    @Override
    public WeatherForecastResult fetchForecast(
        double latitude,
        double longitude,
        LocalDate startDate,
        LocalDate endDate
    ) {
        LocalDate start = startDate != null ? startDate : LocalDate.now(com.travel.trip.service.TripCalendar.ZONE);
        LocalDate end = endDate != null ? endDate : start;
        if (end.isBefore(start) || java.time.temporal.ChronoUnit.DAYS.between(start, end) > 15) throw new AppException(
            ErrorCode.VALIDATION_ERROR
        );

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

        log.debug("Requesting Open-Meteo forecast from {} to {}", start, end);

        OpenMeteoApiResponse response = request(uri);
        try {
            require(response.hourly() != null && timezone.equals(response.timezone()));
            return mapToForecastResult(latitude, longitude, response, start, end);
        } catch (IllegalArgumentException | java.time.DateTimeException | NullPointerException error) {
            throw new AppException(ErrorCode.WEATHER_INVALID_RESPONSE);
        }
    }

    @Override
    public CurrentWeather fetchCurrentWeather(double latitude, double longitude) {
        String baseUrl = weatherConfig.getOpenMeteo().getBaseUrl();
        String timezone = weatherConfig.getOpenMeteo().getDefaultTimezone();

        String uri = UriComponentsBuilder.fromUriString(baseUrl + "/forecast")
            .queryParam("latitude", latitude)
            .queryParam("longitude", longitude)
            .queryParam(
                "current",
                "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,weather_code,wind_speed_10m"
            )
            .queryParam("timezone", timezone)
            .build()
            .toUriString();

        log.debug("Requesting current Open-Meteo weather");

        OpenMeteoApiResponse response = request(uri);
        try {
            require(response.current() != null && timezone.equals(response.timezone()));
            return mapToCurrentWeather(latitude, longitude, response.current());
        } catch (IllegalArgumentException | java.time.DateTimeException | NullPointerException error) {
            throw new AppException(ErrorCode.WEATHER_INVALID_RESPONSE);
        }
    }

    private OpenMeteoApiResponse request(String uri) {
        try {
            // Use the application's Jackson 2 mapper explicitly with Spring 7 RestClient.
            String payload = restClient.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve().body(String.class);
            require(payload != null && payload.length() <= 1_048_576);
            OpenMeteoApiResponse response = json.readValue(payload, OpenMeteoApiResponse.class);
            require(response != null);
            return response;
        } catch (JsonProcessingException error) {
            throw new AppException(ErrorCode.WEATHER_INVALID_RESPONSE);
        } catch (RestClientException error) {
            log.warn("Open-Meteo request unavailable ({})", error.getClass().getSimpleName());
            throw new AppException(ErrorCode.WEATHER_UNAVAILABLE);
        }
    }

    private static void require(boolean valid) {
        if (!valid) throw new AppException(ErrorCode.WEATHER_INVALID_RESPONSE);
    }

    private static boolean hasSize(List<?> values, int expected) {
        return values != null && values.size() == expected;
    }

    private static boolean temperature(double value) {
        return Double.isFinite(value) && value >= -100 && value <= 70;
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
        require(size > 0 && size <= 384);
        require(
            hasSize(temps, size) && hasSize(rainProbs, size) && hasSize(rains, size) && hasSize(weatherCodes, size)
        );
        List<HourlyWeather> allHourly = new ArrayList<>();
        LocalDateTime previous = null;

        for (int i = 0; i < size; i++) {
            LocalDateTime time = LocalDateTime.parse(times.get(i), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            require(!time.toLocalDate().isBefore(startDate) && !time.toLocalDate().isAfter(endDate));
            require(previous == null || time.isAfter(previous));
            previous = time;
            Double temp = temps.get(i);
            Integer rainProb = rainProbs.get(i);
            Double rain = rains.get(i);
            Integer code = weatherCodes.get(i);
            if (temp == null || rainProb == null || rain == null || code == null) continue;
            require(temperature(temp) && rainProb >= 0 && rainProb <= 100 && Double.isFinite(rain) && rain >= 0);

            WeatherCondition condition = WeatherCondition.fromWmoCode(code);
            WeatherRiskLevel risk = WeatherRiskLevel.evaluate(condition, temp, rainProb);

            allHourly.add(new HourlyWeather(time, temp, rainProb, rain, code, condition, risk));
        }

        require(!allHourly.isEmpty());
        // Nhóm các giờ theo từng ngày (LocalDate)
        Map<LocalDate, List<HourlyWeather>> groupedByDate = allHourly
            .stream()
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
            WeatherCondition dominant = hours
                .stream()
                .collect(Collectors.groupingBy(HourlyWeather::condition, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(WeatherCondition.CLEAR_SKY);

            // Tìm mức độ rủi ro cao nhất trong ngày
            WeatherRiskLevel highestRisk = hours
                .stream()
                .map(HourlyWeather::riskLevel)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(WeatherRiskLevel.SAFE);

            dailySummaries.add(
                new DailyWeatherSummary(
                    date,
                    roundOneDecimal(minTemp),
                    roundOneDecimal(maxTemp),
                    maxRainProb,
                    roundOneDecimal(totalRain),
                    dominant,
                    highestRisk,
                    hours
                )
            );
        }

        return new WeatherForecastResult(
            latitude,
            longitude,
            response.timezone() != null ? response.timezone() : "Asia/Ho_Chi_Minh",
            dailySummaries,
            "open-meteo"
        );
    }

    private CurrentWeather mapToCurrentWeather(double latitude, double longitude, OpenMeteoCurrentDto dto) {
        require(
            dto.time() != null &&
                dto.temperature2m() != null &&
                dto.apparentTemperature() != null &&
                dto.relativeHumidity2m() != null &&
                (dto.rain() != null || dto.precipitation() != null) &&
                dto.windSpeed10m() != null &&
                dto.isDay() != null &&
                dto.weatherCode() != null
        );
        LocalDateTime time = LocalDateTime.parse(dto.time(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        double temp = dto.temperature2m();
        double apparentTemp = dto.apparentTemperature();
        int humidity = dto.relativeHumidity2m();
        double rain = dto.rain() != null ? dto.rain() : dto.precipitation();
        double wind = dto.windSpeed10m();
        require(
            temperature(temp) &&
                temperature(apparentTemp) &&
                humidity >= 0 &&
                humidity <= 100 &&
                Double.isFinite(rain) &&
                rain >= 0 &&
                Double.isFinite(wind) &&
                wind >= 0 &&
                (dto.isDay() == 0 || dto.isDay() == 1)
        );
        boolean isDay = dto.isDay() == 1;
        int code = dto.weatherCode();
        WeatherCondition condition = WeatherCondition.fromWmoCode(code);
        WeatherRiskLevel risk = WeatherRiskLevel.evaluate(condition, temp, rain > 0 ? 80 : 10);

        return new CurrentWeather(
            latitude,
            longitude,
            time,
            temp,
            apparentTemp,
            humidity,
            rain,
            wind,
            isDay,
            code,
            condition,
            risk,
            null
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
        OpenMeteoHourlyDto hourly,
        OpenMeteoCurrentDto current
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoHourlyDto(
        List<String> time,
        @JsonProperty("temperature_2m") List<Double> temperature2m,
        @JsonProperty("precipitation_probability") List<Integer> precipitationProbability,
        List<Double> rain,
        @JsonProperty("weather_code") List<Integer> weatherCode
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OpenMeteoCurrentDto(
        String time,
        @JsonProperty("temperature_2m") Double temperature2m,
        @JsonProperty("relative_humidity_2m") Integer relativeHumidity2m,
        @JsonProperty("apparent_temperature") Double apparentTemperature,
        @JsonProperty("is_day") Integer isDay,
        Double precipitation,
        Double rain,
        @JsonProperty("weather_code") Integer weatherCode,
        @JsonProperty("wind_speed_10m") Double windSpeed10m
    ) {}
}
