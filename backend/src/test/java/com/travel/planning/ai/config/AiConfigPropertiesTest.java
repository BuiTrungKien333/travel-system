package com.travel.planning.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiConfigPropertiesTest {

    @Test
    @DisplayName("Default properties should be properly initialized")
    void testDefaultProperties() {
        AiConfigProperties properties = new AiConfigProperties();

        assertThat(properties.getActiveProvider()).isEqualTo("gemini");
        assertThat(properties.getGemini()).isNotNull();
        assertThat(properties.getGemini().getModel()).isEqualTo("gemini-3.5-flash");
        assertThat(properties.getGemini().getBaseUrl()).isEqualTo("https://generativelanguage.googleapis.com/v1beta");
        assertThat(properties.getGemini().getTimeoutSeconds()).isEqualTo(30);
        assertThat(properties.getGemini().getTemperature()).isEqualTo(0.4);
        assertThat(properties.getGemini().getMaxOutputTokens()).isEqualTo(4096);
    }
}
