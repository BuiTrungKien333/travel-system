package com.travel.common.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {
    private final boolean success;
    private final String code;
    private final String message;
    private final Map<String, String> errors;
    private final Instant timestamp;
}

