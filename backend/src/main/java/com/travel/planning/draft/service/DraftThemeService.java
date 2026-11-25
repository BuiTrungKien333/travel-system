package com.travel.planning.draft.service;

import com.travel.planning.draft.dto.response.DraftResponse.Day;
import com.travel.planning.draft.dto.response.DraftResponse.Stop;
import com.travel.planning.draft.model.DraftAllocation;
import com.travel.planning.draft.model.DraftAllocation.ThemeHint;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Render only the final schedule. Reusing allocation hints avoids another LLM round trip.
 */
@Service
@Profile({ "dev", "rag-demo", "planning-ai" })
public class DraftThemeService {

    public Narration narrate(List<Day> days, DraftAllocation allocation) {
        List<Day> result = new ArrayList<>();
        int aiDays = 0;
        for (var day : days) {
            ThemeHint hint = allocation.themes().get(day.dayIndex());
            boolean useAi = valid(hint, day);
            if (useAi) aiDays++;
            List<String> names = day
                .stops()
                .stream()
                .filter(s -> !useAi || hint.highlights().contains(s.place().placeId()))
                .limit(3)
                .map(s -> s.place().name())
                .toList();
            result.add(
                render(
                    day,
                    useAi ? hint.theme() : "MIXED",
                    names,
                    useAi ? "AI_GROUNDED_TEMPLATE" : "RULE_BASED_FALLBACK"
                )
            );
        }
        String source = aiDays == days.size() ? "AI" : aiDays == 0 ? "FALLBACK" : "AI_WITH_FALLBACK";
        String reason =
            aiDays == days.size()
                ? null
                : allocation.reason() == null
                  ? "THEME_HINT_INVALID_FOR_FINAL_SCHEDULE"
                  : allocation.reason();
        return new Narration(List.copyOf(result), source, reason);
    }

    private boolean valid(ThemeHint hint, Day day) {
        if (
            hint == null || hint.highlights().size() > 3 || hint.highlights().isEmpty() != day.stops().isEmpty()
        ) return false;
        Set<UUID> ids = new HashSet<>();
        Set<String> tags = new HashSet<>();
        for (Stop stop : day.stops()) {
            ids.add(stop.place().placeId());
            tags.addAll(stop.place().tags());
        }
        if (
            !ids.containsAll(hint.highlights()) || new HashSet<>(hint.highlights()).size() != hint.highlights().size()
        ) return false;
        return switch (hint.theme()) {
            case "CULTURE_ART" -> !Collections.disjoint(tags, Set.of("CULTURE", "ART", "MUSEUM"));
            case "HISTORY" -> tags.contains("HISTORY");
            case "SEA_SCENERY" -> !Collections.disjoint(tags, Set.of("BEACH", "SCENERY", "PARK"));
            case "LOCAL_MARKETS" -> !Collections.disjoint(tags, Set.of("MARKET", "SHOPPING", "LOCAL"));
            case "MIXED" -> !day.stops().isEmpty();
            case "FREE_TIME" -> day.stops().isEmpty();
            default -> false;
        };
    }

    private Day render(Day day, String theme, List<String> names, String source) {
        String label = switch (theme) {
            case "CULTURE_ART" -> "Văn hóa và nghệ thuật";
            case "HISTORY" -> "Tìm hiểu lịch sử";
            case "SEA_SCENERY" -> "Biển và cảnh quan";
            case "LOCAL_MARKETS" -> "Khám phá địa phương";
            default -> day.stops().isEmpty() ? "Thời gian tự do" : "Khám phá địa phương";
        };
        String summary = names.isEmpty()
            ? "Chưa xếp điểm tham quan cho ngày này."
            : "Lịch có " + day.stops().size() + " điểm dừng; điểm nhấn: " + String.join(", ", names) + ".";
        return day.narrated("Ngày " + day.dayIndex() + " — " + label, summary, source);
    }

    public record Narration(List<Day> days, String source, String fallbackReason) {}
}
