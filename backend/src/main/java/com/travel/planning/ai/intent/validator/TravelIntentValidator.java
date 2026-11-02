package com.travel.planning.ai.intent.validator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.intent.model.TravelIntent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;

@Component
@Profile("rag-demo")
public class TravelIntentValidator {
    public static final Set<String> TAGS = Set.of("MUSEUM", "HISTORY", "CULTURE", "ART", "LOCAL",
            "MARKET", "SHOPPING", "BEACH", "SCENERY", "RELAX", "OUTDOOR", "PARK", "ARCHITECTURE", "TEMPLE");
    private static final Set<String> ARRAYS = Set.of("interests", "excludedTags", "mustVisitNames", "excludedPlaceNames");
    private static final Set<String> FIELDS = Set.of("destination", "numberOfDays", "interests", "excludedTags",
            "dailyStartTime", "dailyEndTime", "travelPace", "mustVisitNames", "excludedPlaceNames");
    private final ObjectMapper mapper;

    public TravelIntentValidator(ObjectMapper mapper) { this.mapper = mapper; }

    public TravelIntent validate(String json, String source) {
        try {
            JsonNode root = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json);
            if (root == null || !root.isObject() || root.size() != FIELDS.size() + 1) throw invalid();
            JsonNode evidence = root.get("evidence");
            if (evidence == null || !evidence.isObject() || evidence.size() != FIELDS.size()) throw invalid();
            for (String field : FIELDS) {
                JsonNode value = root.get(field), quote = evidence.get(field);
                if (value == null || quote == null || (!quote.isNull() && !quote.isTextual())) throw invalid();
                boolean supplied;
                if (ARRAYS.contains(field)) {
                    if (!value.isArray() || value.size() > 10) throw invalid();
                    for (JsonNode item : value) {
                        if (!item.isTextual() || item.asText().isBlank() || item.asText().length() > 160) throw invalid();
                        if ((field.equals("interests") || field.equals("excludedTags")) && !TAGS.contains(item.asText())) throw invalid();
                    }
                    supplied = !value.isEmpty();
                } else if (field.equals("numberOfDays")) {
                    if (!value.isNull() && (!value.isIntegralNumber() || !value.canConvertToInt()
                            || value.intValue() < 1 || value.intValue() > 30)) throw invalid();
                    supplied = !value.isNull();
                } else {
                    if (!value.isNull() && (!value.isTextual() || value.asText().isBlank() || value.asText().length() > 160)) throw invalid();
                    supplied = !value.isNull();
                }
                if (supplied && (quote.isNull() || quote.asText().isBlank() || !source.contains(quote.asText()))) throw invalid();
                if (!supplied && !quote.isNull()) throw invalid();
            }
            TravelIntent intent = mapper.treeToValue(root, TravelIntent.class);
            for (String time : Arrays.asList(intent.dailyStartTime(), intent.dailyEndTime())) {
                if (time != null) {
                    if (!time.matches("\\d{2}:\\d{2}")) throw invalid();
                    LocalTime.parse(time);
                }
            }
            if (intent.travelPace() != null && !Set.of("RELAXED", "BALANCED", "ACTIVE").contains(intent.travelPace())) throw invalid();
            return new TravelIntent(intent.destination(), intent.numberOfDays(), unique(intent.interests()),
                    unique(intent.excludedTags()), intent.dailyStartTime(), intent.dailyEndTime(), intent.travelPace(),
                    unique(intent.mustVisitNames()), unique(intent.excludedPlaceNames()), intent.evidence());
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid();
        }
    }

    private List<String> unique(List<String> items) { return List.copyOf(new LinkedHashSet<>(items)); }
    private AppException invalid() { return new AppException(ErrorCode.AI_VALIDATION_FAILED); }
}
