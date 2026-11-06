package com.travel.trip.dto;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public final class TripDateChangeRequest {
    @NotNull @Min(0) private Long version;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    @JsonSetter(nulls = Nulls.FAIL) public void setVersion(Object value) { version = TripVersion.parse(value); }
    public void setStartDate(LocalDate value) { startDate = value; }
    public void setEndDate(LocalDate value) { endDate = value; }
    public long version() { return version; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    @JsonAnySetter public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unknown date preview field"); }
}
