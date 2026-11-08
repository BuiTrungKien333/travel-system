package com.travel.planning.ai.dto.request.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<GeminiCandidate> candidates
) {
    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        GeminiCandidate first = candidates.getFirst();
        if (first.content() == null || first.content().parts() == null || first.content().parts().isEmpty()) {
            return null;
        }
        return first.content().parts().getFirst().text();
    }
}
