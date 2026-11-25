package com.travel.trip.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class CreateTripRequest {

    @NotNull
    private UUID clientRequestId;

    @JsonSetter(nulls = Nulls.FAIL)
    @Size(max = 120)
    private String title = "";

    @NotNull
    @Valid
    private Destination destination;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @JsonSetter(nulls = Nulls.FAIL)
    @Pattern(regexp = "Asia/Ho_Chi_Minh")
    private String timeZone = "Asia/Ho_Chi_Minh";

    @Valid
    private Budget budget;

    @JsonSetter(nulls = Nulls.FAIL)
    @Size(max = 8)
    private List<@NotNull Preference> preferences = List.of();

    @Size(max = 200)
    private String accommodation;

    @Size(max = 1000)
    private String notes;

    @JsonSetter(nulls = Nulls.FAIL)
    @Size(max = 100)
    private List<@NotNull UUID> initialPlaceIds = List.of();

    @JsonSetter(nulls = Nulls.FAIL)
    public void setTitle(String value) {
        title = value.trim();
    }

    public void setAccommodation(String value) {
        accommodation = clean(value);
    }

    public void setNotes(String value) {
        notes = clean(value);
    }

    public UUID clientRequestId() {
        return clientRequestId;
    }

    public String title() {
        return title.trim();
    }

    public Destination destination() {
        return destination;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public String timeZone() {
        return timeZone;
    }

    public Budget budget() {
        return budget;
    }

    public List<Preference> preferences() {
        return preferences;
    }

    public String accommodation() {
        return clean(accommodation);
    }

    public String notes() {
        return clean(notes);
    }

    public List<UUID> initialPlaceIds() {
        return initialPlaceIds;
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown trip field");
    }

    public record Destination(@NotBlank @Size(max = 120) String name, UUID provinceId) {
        public Destination {
            name = name == null ? null : name.trim();
        }

        @JsonAnySetter
        public void rejectUnknown(String field, Object value) {
            throw new IllegalArgumentException("Unknown destination field");
        }
    }

    public record Budget(
        @NotNull @DecimalMin("0") @DecimalMax("1000000000000") @Digits(integer = 13, fraction = 0) BigDecimal amount,
        @NotNull @Pattern(regexp = "VND") String currency
    ) {
        @JsonAnySetter
        public void rejectUnknown(String field, Object value) {
            throw new IllegalArgumentException("Unknown budget field");
        }
    }

    public enum Preference {
        nature,
        food,
        culture,
        relaxation,
        photography,
        adventure,
        shopping,
        beach;

        @JsonCreator
        public static Preference from(String value) {
            return valueOf(value);
        }
    }
}
