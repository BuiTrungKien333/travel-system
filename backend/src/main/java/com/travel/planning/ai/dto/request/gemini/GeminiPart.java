package com.travel.planning.ai.dto.request.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiPart(String text) {}
