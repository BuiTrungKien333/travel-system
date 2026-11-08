package com.travel.planning.ai.dto.request.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiCandidate(
        GeminiContent content,
        String finishReason
) {
}
