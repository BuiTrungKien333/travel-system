package com.travel.weather.provider;

import com.travel.weather.config.WeatherConfigProperties;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WeatherProviderFactory {

    private final WeatherConfigProperties weatherConfig;
    private final Map<String, WeatherProvider> providerMap;

    public WeatherProviderFactory(WeatherConfigProperties weatherConfig, List<WeatherProvider> providers) {
        this.weatherConfig = weatherConfig;
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toLowerCase(),
                        Function.identity()
                ));
        log.info("[WEATHER_FACTORY] Registered weather providers: {}", providerMap.keySet());
    }

    /**
     * Lấy WeatherProvider theo cấu hình application.weather.active-provider
     */
    public WeatherProvider getProvider() {
        String active = weatherConfig.getActiveProvider() != null
                ? weatherConfig.getActiveProvider().trim().toLowerCase()
                : "open-meteo";

        WeatherProvider provider = providerMap.get(active);
        if (provider == null) {
            throw new AppException(ErrorCode.WEATHER_UNAVAILABLE);
        }

        if (!provider.isAvailable()) {
            throw new AppException(ErrorCode.WEATHER_UNAVAILABLE);
        }

        return provider;
    }

    /**
     * Lấy WeatherProvider theo tên chỉ định
     */
    public Optional<WeatherProvider> getProvider(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(providerMap.get(name.trim().toLowerCase()));
    }

}
