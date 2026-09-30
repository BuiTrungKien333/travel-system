package com.travel.weather.provider;

import com.travel.weather.config.WeatherConfigProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class OpenMeteoWeatherProviderContextTest {

    @Test
    @DisplayName("Spring ApplicationContext should successfully instantiate OpenMeteoWeatherProvider with @Autowired constructor")
    void springContext_instantiatesOpenMeteoWeatherProviderSuccessfully() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(WeatherConfigProperties.class, WeatherConfigProperties::new);
            ctx.register(MockWeatherProvider.class);
            ctx.register(OpenMeteoWeatherProvider.class);
            ctx.register(WeatherProviderFactory.class);
            ctx.refresh();

            OpenMeteoWeatherProvider provider = ctx.getBean(OpenMeteoWeatherProvider.class);
            assertThat(provider).isNotNull();
            assertThat(provider.getProviderName()).isEqualTo("open-meteo");

            WeatherProviderFactory factory = ctx.getBean(WeatherProviderFactory.class);
            assertThat(factory).isNotNull();
        }
    }
}
