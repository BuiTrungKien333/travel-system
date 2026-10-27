package com.travel.trip.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateTripRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 255) String destination,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 50) String transportation,
        @Size(max = 30) List<@NotBlank @Size(max = 100) String> preferences,
        @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal budget,
        @Size(max = 2000) String constraints,
        @Size(max = 10000) String description,
        @Size(max = 2048) @Pattern(regexp = "^$|https?://\\S+") String coverImage
) {}
