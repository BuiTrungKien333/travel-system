package com.travel.planning.ai.dto.request.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiGenerationConfig(
        Double temperature,
        Integer maxOutputTokens,
        String responseMimeType
) {
    public static GeminiGenerationConfig jsonConfig(Double temperature, Integer maxOutputTokens) {
        return new GeminiGenerationConfig(temperature, maxOutputTokens, "application/json");
    }
}
