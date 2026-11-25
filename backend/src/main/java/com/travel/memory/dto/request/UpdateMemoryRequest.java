package com.travel.memory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMemoryRequest(@NotNull @Min(0) Long version, @NotNull @Size(max = 2000) String note) {}
