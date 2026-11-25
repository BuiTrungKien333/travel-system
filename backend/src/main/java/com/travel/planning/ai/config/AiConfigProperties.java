package com.travel.planning.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application.ai")
@Getter
@Setter
public class AiConfigProperties {

    /**
     * Provider AI được kích hoạt: "gemini", "mock", "openai"
     */
    private String activeProvider = "gemini";

    /**
     * Cấu hình Google Gemini AI
     */
    private GeminiConfig gemini = new GeminiConfig();

    @Getter
    @Setter
    public static class GeminiConfig {

        /**
         * Google Gemini API Key
         */
        private String apiKey;

        /**
         * Model name (Mặc định: gemini-3.5-flash)
         */
        private String model = "gemini-3.5-flash";

        /**
         * URL gốc của Gemini REST API
         */
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

        /**
         * Thời gian chờ timeout (giây)
         */
        private int timeoutSeconds = 30;

        /**
         * Nhiệt độ sáng tạo (0.0 - 1.0)
         */
        private double temperature = 0.4;

        /**
         * Số lượng token đầu ra tối đa
         */
        private int maxOutputTokens = 4096;
    }
}
