package com.travel.place.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DestinationPage(List<Option> content, int page, int size, long totalElements,
                              long totalPages, Context context) {
    public record Option(UUID id, String code, String name) { }
    public record Context(Instant serverTime, LocalDate evaluatedDate) { }
}
