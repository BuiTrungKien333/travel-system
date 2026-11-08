package com.travel.planning.ai.dto.request.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiContent(
        String role,
        List<GeminiPart> parts
) {
    public static GeminiContent user(String text) {
        return new GeminiContent("user", List.of(new GeminiPart(text)));
    }

    public static GeminiContent system(String text) {
        return new GeminiContent(null, List.of(new GeminiPart(text)));
    }
}
