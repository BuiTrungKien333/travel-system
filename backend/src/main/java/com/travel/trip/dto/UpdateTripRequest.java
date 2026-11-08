package com.travel.trip.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.travel.trip.dto.CreateTripRequest.Preference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Presence is separate from value: omitted fields stay unchanged; explicit null clears nullable metadata.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY
)
public final class UpdateTripRequest {

    @NotNull
    @Min(0)
    private Long version;

    @Size(max = 120)
    private String title;

    @Valid
    private CreateTripRequest.Destination destination;

    private LocalDate startDate;
    private LocalDate endDate;

    @Valid
    private CreateTripRequest.Budget budget;

    @Size(max = 8)
    private List<@NotNull Preference> preferences;

    @Size(max = 200)
    private String accommodation;

    @Size(max = 1000)
    private String notes;

    private boolean acknowledgeDateImpact;
    private final Set<String> fields = new HashSet<>();

    @JsonSetter(nulls = Nulls.FAIL)
    public void setVersion(Object value) {
        version = TripVersion.parse(value);
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setTitle(String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        title = value.trim();
        fields.add("title");
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setDestination(CreateTripRequest.Destination value) {
        destination = value;
        fields.add("destination");
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setStartDate(LocalDate value) {
        startDate = value;
        fields.add("startDate");
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setEndDate(LocalDate value) {
        endDate = value;
        fields.add("endDate");
    }

    public void setBudget(CreateTripRequest.Budget value) {
        budget = value;
        fields.add("budget");
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setPreferences(List<CreateTripRequest.Preference> value) {
        preferences = value;
        fields.add("preferences");
    }

    public void setAccommodation(String value) {
        accommodation = clean(value);
        fields.add("accommodation");
    }

    public void setNotes(String value) {
        notes = clean(value);
        fields.add("notes");
    }

    @JsonSetter(nulls = Nulls.FAIL)
    public void setAcknowledgeDateImpact(boolean value) {
        acknowledgeDateImpact = value;
    }

    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown trip field");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public boolean has(String field) {
        return fields.contains(field);
    }

    public boolean hasChanges() {
        return !fields.isEmpty();
    }

    public long version() {
        return version;
    }

    public String title() {
        return title;
    }

    public CreateTripRequest.Destination destination() {
        return destination;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate endDate() {
        return endDate;
    }

    public CreateTripRequest.Budget budget() {
        return budget;
    }

    public List<CreateTripRequest.Preference> preferences() {
        return preferences;
    }

    public String accommodation() {
        return accommodation;
    }

    public String notes() {
        return notes;
    }

    public boolean acknowledgeDateImpact() {
        return acknowledgeDateImpact;
    }
}
