package com.travel.planning.draft.dto.request;

import com.travel.planning.ai.intent.model.TravelIntent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.*;

public record GenerateDraftRequest(
        @Size(max = 1000) String text,
        @NotNull LocalDate startDate,
        @NotNull @Valid Origin origin,
        Boolean roundTrip,
        @Min(0) @Max(90) Integer restMinutes,
        @Valid Preferences preferences) {

    public GenerateDraftRequest(String text, LocalDate startDate, Origin origin, Boolean roundTrip, Integer restMinutes) {
        this(text, startDate, origin, roundTrip, restMinutes, null);
    }

    public GenerateDraftRequest {
        if (roundTrip == null) roundTrip = true;
        if (restMinutes == null) restMinutes = 45;
    }

    @AssertTrue(message = "Cung cấp đúng một trong text hoặc preferences")
    public boolean isInputExclusive() {
        return (text != null && !text.isBlank()) != (preferences != null);
    }

    public record Origin(@NotBlank @Size(max = 160) String name,
                         @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
                         @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {
    }

    public enum Tag { MUSEUM, HISTORY, CULTURE, ART, LOCAL, MARKET, SHOPPING, BEACH, SCENERY, RELAX, OUTDOOR, PARK, ARCHITECTURE, TEMPLE }
    public enum Pace { RELAXED, BALANCED, ACTIVE }

    /** Explicit user input; never presented as an AI-extracted intent. */
    public record Preferences(@NotBlank @Size(max = 160) String destination,
                              @NotNull @Min(1) @Max(2) Integer numberOfDays,
                              @Size(max = 10) List<@NotNull Tag> interests,
                              @Size(max = 10) List<@NotNull Tag> excludedTags,
                              @NotNull @Pattern(regexp = "(?:[01][0-9]|2[0-3]):[0-5][0-9]") String dailyStartTime,
                              @NotNull @Pattern(regexp = "(?:[01][0-9]|2[0-3]):[0-5][0-9]") String dailyEndTime,
                              Pace travelPace,
                              @Size(max = 10) List<@NotBlank @Size(max = 160) String> mustVisitNames,
                              @Size(max = 10) List<@NotBlank @Size(max = 160) String> excludedPlaceNames) {
        public Preferences {
            interests = interests == null ? List.of() : interests;
            excludedTags = excludedTags == null ? List.of() : excludedTags;
            mustVisitNames = mustVisitNames == null ? List.of() : mustVisitNames;
            excludedPlaceNames = excludedPlaceNames == null ? List.of() : excludedPlaceNames;
        }

        public TravelIntent toIntent() {
            return new TravelIntent(destination, numberOfDays, interests.stream().map(Enum::name).distinct().toList(),
                    excludedTags.stream().map(Enum::name).distinct().toList(), dailyStartTime, dailyEndTime,
                    travelPace == null ? null : travelPace.name(), mustVisitNames.stream().distinct().toList(),
                    excludedPlaceNames.stream().distinct().toList(), Map.of());
        }
    }
}
