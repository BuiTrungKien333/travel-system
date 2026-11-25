package com.travel.trip.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY
)
public final class CompleteTripRequest {

    @NotNull
    @Min(0)
    private Long version;

    @JsonSetter(nulls = Nulls.FAIL)
    public void setVersion(Object value) {
        version = TripVersion.parse(value);
    }

    public long version() {
        return version;
    }

    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown completion field");
    }
}
