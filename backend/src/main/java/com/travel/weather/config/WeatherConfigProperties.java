package com.travel.weather.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application.weather")
@Getter
@Setter
public class WeatherConfigProperties {

    /**
     * Provider thời tiết được kích hoạt: "open-meteo", "mock", "openweathermap"
     */
    private String activeProvider = "open-meteo";

    /**
     * Cấu hình Open-Meteo Provider (Mặc định miễn phí, không cần key)
     */
    private OpenMeteoConfig openMeteo = new OpenMeteoConfig();

    @Getter
    @Setter
    public static class OpenMeteoConfig {

        private String baseUrl = "https://api.open-meteo.com/v1";
        private int timeoutSeconds = 10;
        private String defaultTimezone = "Asia/Ho_Chi_Minh";
    }
}
