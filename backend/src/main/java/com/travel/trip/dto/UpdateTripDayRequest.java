package com.travel.trip.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateTripDayRequest {
    @NotNull @PositiveOrZero private Long version;
    @Size(max = 200) private String startLocation;
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") private String startTime;
    @NotNull private Boolean returnToStart;
    @NotNull private Transport transport;
    private Instant expectedItineraryUpdatedAt;
    @Size(max = 100) private List<@NotNull @Valid VisitDuration> durations = List.of();

    @JsonSetter(nulls = Nulls.FAIL)
    public void setVersion(Object value) { version = TripVersion.parse(value); }

    public void setStartLocation(String value) {
        startLocation = value == null || value.isBlank() ? null : value.trim();
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setDurations(List<VisitDuration> value) { durations = value; }

    @JsonAnySetter
    public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unknown day field"); }

    public enum Transport { CAR, MOTORCYCLE, WALK }
    public record VisitDuration(@NotNull UUID stopId, @NotNull @Min(5) @Max(480) Integer durationMinutes) { }
}
