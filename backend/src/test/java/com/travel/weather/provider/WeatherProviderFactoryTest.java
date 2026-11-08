package com.travel.weather.provider;

import com.travel.weather.config.WeatherConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherProviderFactoryTest {

    @Mock
    private WeatherProvider openMeteoProvider;

    @Mock
    private WeatherProvider mockProvider;

    private WeatherConfigProperties properties;
    private WeatherProviderFactory factory;

    @BeforeEach
    void setUp() {
        when(openMeteoProvider.getProviderName()).thenReturn("open-meteo");
        when(mockProvider.getProviderName()).thenReturn("mock");

        properties = new WeatherConfigProperties();
        factory = new WeatherProviderFactory(properties, List.of(openMeteoProvider, mockProvider));
    }

    @Test
    @DisplayName("getProvider should return open-meteo provider when configured and available")
    void getProvider_openMeteoConfigured_returnsOpenMeteo() {
        properties.setActiveProvider("open-meteo");
        when(openMeteoProvider.isAvailable()).thenReturn(true);

        WeatherProvider provider = factory.getProvider();

        assertThat(provider).isEqualTo(openMeteoProvider);
    }

    @Test
    @DisplayName("getProvider should report unavailable rather than silently switching to mock")
    void getProvider_unavailable_throws() {
        properties.setActiveProvider("open-meteo");
        when(openMeteoProvider.isAvailable()).thenReturn(false);

        assertThatThrownBy(factory::getProvider).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WEATHER_UNAVAILABLE));
    }

    @Test
    @DisplayName("getProvider with specific name should return requested provider")
    void getProvider_byName_returnsMatchingProvider() {
        Optional<WeatherProvider> result = factory.getProvider("mock");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockProvider);
    }
}
