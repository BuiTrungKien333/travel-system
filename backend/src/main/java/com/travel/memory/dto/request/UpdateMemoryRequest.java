package com.travel.memory.dto.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
public class UpdateMemoryRequest {
    @NotNull private Long version;
    @Size(max = 2000) private String note;
    @PastOrPresent private Instant experiencedAt;
    @Size(max = 100) private List<@NotNull UUID> mediaOrder;
    @JsonIgnore private boolean noteProvided;

    @JsonSetter
    public void setVersion(Object value) {
        if (!(value instanceof Integer || value instanceof Long) || ((Number) value).longValue() < 0)
            throw new IllegalArgumentException("version must be a nonnegative integer");
        version = ((Number) value).longValue();
    }
    @JsonSetter public void setNote(String value) { note = value; noteProvided = true; }
    @JsonSetter(nulls = Nulls.FAIL) public void setExperiencedAt(Instant value) { experiencedAt = value; }
    @JsonSetter(nulls = Nulls.FAIL) public void setMediaOrder(List<UUID> value) { mediaOrder = value; }
    @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Unknown memory field"); }
}
