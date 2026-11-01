package com.travel.planning.ai.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class TravelIntentValidatorTest {
    private final TravelIntentValidator validator = new TravelIntentValidator(new ObjectMapper());

    @Test void validatesVietnamesePreferencesAndExclusionWithEvidence() throws Exception {
        var result = validator.validate(IntentFixtures.json().toString(), IntentFixtures.TEXT);
        assertThat(result.numberOfDays()).isEqualTo(2);
        assertThat(result.interests()).containsExactly("HISTORY", "ART");
        assertThat(result.excludedTags()).containsExactly("BEACH");
        assertThat(result.dailyStartTime()).isEqualTo("08:00");
        assertThat(result.evidence().get("excludedTags")).isEqualTo("không đi biển");
    }

    @Test void missingFieldsRemainNullWithoutInventedDefaults() throws Exception {
        var root = IntentFixtures.json();
        for (String key : new String[]{"destination", "numberOfDays", "dailyStartTime", "dailyEndTime", "travelPace"}) {
            root.putNull(key); ((com.fasterxml.jackson.databind.node.ObjectNode) root.get("evidence")).putNull(key);
        }
        var result = validator.validate(root.toString(), IntentFixtures.TEXT);
        assertThat(result.destination()).isNull(); assertThat(result.numberOfDays()).isNull();
        assertThat(result.dailyStartTime()).isNull(); assertThat(result.travelPace()).isNull();
    }

    @ParameterizedTest @ValueSource(strings={"25:00", "8:00", "08:00:00", "tomorrow"})
    void rejectsInvalidTime(String value) throws Exception {
        var root = IntentFixtures.json(); root.put("dailyStartTime", value);
        assertThatThrownBy(() -> validator.validate(root.toString(), IntentFixtures.TEXT)).isInstanceOf(AppException.class);
    }

    @Test void rejectsInventedEvidenceAndUnknownTagAndWrongType() throws Exception {
        var root = IntentFixtures.json(); root.withObject("evidence").put("destination", "Hà Nội");
        assertThatThrownBy(() -> validator.validate(root.toString(), IntentFixtures.TEXT))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_VALIDATION_FAILED));
        root.withObject("evidence").put("destination", "Đà Nẵng");
        root.putArray("interests").add("FAKE_TAG");
        assertThatThrownBy(() -> validator.validate(root.toString(), IntentFixtures.TEXT)).isInstanceOf(AppException.class);
        root.putArray("interests").add("ART"); root.put("numberOfDays", "2");
        assertThatThrownBy(() -> validator.validate(root.toString(), IntentFixtures.TEXT)).isInstanceOf(AppException.class);
    }

    @Test void rejectsExtraMissingAndTrailingJson() throws Exception {
        var root = IntentFixtures.json(); root.put("placeId", "invented");
        assertThatThrownBy(() -> validator.validate(root.toString(), IntentFixtures.TEXT)).isInstanceOf(AppException.class);
        root.remove("placeId"); root.remove("destination");
        assertThatThrownBy(() -> validator.validate(root.toString(), IntentFixtures.TEXT)).isInstanceOf(AppException.class);
        String trailing = IntentFixtures.json() + " {}";
        assertThatThrownBy(() -> validator.validate(trailing, IntentFixtures.TEXT)).isInstanceOf(AppException.class);
    }
}
