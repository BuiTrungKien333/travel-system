package com.travel.planning.ai.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.ai.intent.validator.TravelIntentValidator;

import java.util.*;

public final class IntentFixtures {
    public static final String TEXT = "Tôi đi Đà Nẵng hai ngày, thích lịch sử và nghệ thuật, không đi biển, mỗi ngày từ 8 giờ đến 17 giờ, đi thong thả.";
    private IntentFixtures() {}

    public static ObjectNode json() throws Exception {
        var mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("destination", "Đà Nẵng").put("numberOfDays", 2);
        root.putArray("interests").add("HISTORY").add("ART");
        root.putArray("excludedTags").add("BEACH");
        root.put("dailyStartTime", "08:00").put("dailyEndTime", "17:00").put("travelPace", "RELAXED");
        root.putArray("mustVisitNames"); root.putArray("excludedPlaceNames");
        ObjectNode evidence = root.putObject("evidence");
        evidence.put("destination", "Đà Nẵng").put("numberOfDays", "hai ngày").put("interests", "lịch sử và nghệ thuật")
                .put("excludedTags", "không đi biển").put("dailyStartTime", "8 giờ").put("dailyEndTime", "17 giờ")
                .put("travelPace", "thong thả").putNull("mustVisitNames").putNull("excludedPlaceNames");
        return root;
    }

    public static TravelIntent extracted() throws Exception {
        return new TravelIntentValidator(new ObjectMapper()).validate(json().toString(), TEXT);
    }

    public static TravelIntent intent(String destination, List<String> interests, List<String> exclusions,
                                      List<String> required, List<String> excludedNames) {
        return new TravelIntent(destination, null, interests, exclusions, null, null, null, required, excludedNames, Map.of());
    }
}
