package com.travel.planning.ai.provider;

import com.travel.planning.ai.config.AiConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProviderFactoryTest {

    @Mock
    private AiProvider geminiProvider;

    @Mock
    private AiProvider mockProvider;

    private AiConfigProperties properties;
    private AiProviderFactory factory;

    @BeforeEach
    void setUp() {
        when(geminiProvider.getProviderName()).thenReturn("gemini");
        when(mockProvider.getProviderName()).thenReturn("mock");

        properties = new AiConfigProperties();
        factory = new AiProviderFactory(properties, List.of(geminiProvider, mockProvider));
    }

    @Test
    @DisplayName("getProvider should return gemini provider when configured and available")
    void getProvider_geminiConfiguredAndAvailable_returnsGemini() {
        properties.setActiveProvider("gemini");
        when(geminiProvider.isAvailable()).thenReturn(true);

        AiProvider provider = factory.getProvider();

        assertThat(provider).isEqualTo(geminiProvider);
    }

    @Test
    @DisplayName("getProvider should fallback to mock provider when gemini is unavailable")
    void getProvider_geminiUnavailable_fallsBackToMock() {
        properties.setActiveProvider("gemini");
        when(geminiProvider.isAvailable()).thenReturn(false);

        AiProvider provider = factory.getProvider();

        assertThat(provider).isEqualTo(mockProvider);
    }

    @Test
    @DisplayName("getProvider with specific name should return requested provider")
    void getProvider_byName_returnsMatchingProvider() {
        Optional<AiProvider> result = factory.getProvider("mock");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockProvider);
    }
}
