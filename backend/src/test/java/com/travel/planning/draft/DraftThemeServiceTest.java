package com.travel.planning.draft;

import static com.travel.planning.draft.DraftFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.planning.algorithm.*;
import com.travel.planning.draft.model.DraftAllocation;
import com.travel.planning.draft.model.DraftAllocation.ThemeHint;
import com.travel.planning.draft.service.DraftThemeService;
import com.travel.planning.draft.service.DraftTimelinePlanner;
import com.travel.planning.timeline.TimelineEngine;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.Test;

class DraftThemeServiceTest {

    final DraftThemeService service = new DraftThemeService();
    final DraftTimelinePlanner planner = new DraftTimelinePlanner(
        new HeldKarpSolver(new HaversineDistanceMatrixService()),
        new TimelineEngine(),
        new ObjectMapper()
    );

    @Test
    void validAllocationHintRendersGroundedThemeWithoutAnotherProviderCall() {
        var point = candidate(1);
        var day = planner.build(
            1,
            DATE,
            LocalTime.of(8, 0),
            LocalTime.of(17, 0),
            ORIGIN,
            true,
            0,
            0,
            List.of(point),
            entities(List.of(point)),
            travel(List.of(point))
        );
        var allocation = new DraftAllocation(
            List.of(List.of(point.placeId())),
            Map.of(1, new ThemeHint("CULTURE_ART", List.of(point.placeId()))),
            "AI",
            null
        );
        var result = service.narrate(List.of(day), allocation);
        assertThat(result.source()).isEqualTo("AI");
        assertThat(result.days().getFirst().summarySource()).isEqualTo("AI_GROUNDED_TEMPLATE");
        assertThat(result.days().getFirst().summary()).contains(point.name());
        assertThat(result.fallbackReason()).isNull();
    }

    @Test
    void repairedScheduleDoesNotReuseThemeReferencesFromRemovedPlaces() {
        var kept = candidate(1);
        var removed = candidate(2);
        var day = planner.build(
            1,
            DATE,
            LocalTime.of(8, 0),
            LocalTime.of(17, 0),
            ORIGIN,
            true,
            0,
            0,
            List.of(kept),
            entities(List.of(kept)),
            travel(List.of(kept))
        );
        var allocation = new DraftAllocation(
            List.of(List.of(kept.placeId(), removed.placeId())),
            Map.of(1, new ThemeHint("CULTURE_ART", List.of(removed.placeId()))),
            "AI",
            null
        );
        var result = service.narrate(List.of(day), allocation);
        assertThat(result.source()).isEqualTo("FALLBACK");
        assertThat(result.days().getFirst().summary()).contains(kept.name()).doesNotContain(removed.name());
        assertThat(result.fallbackReason()).isEqualTo("THEME_HINT_INVALID_FOR_FINAL_SCHEDULE");
    }

    @Test
    void themeMustBeSupportedByFinalPlaceTagsAndMixedSourcesAreExplicit() {
        var points = List.of(candidate(1), candidate(2));
        var first = planner.build(
            1,
            DATE,
            LocalTime.of(8, 0),
            LocalTime.of(17, 0),
            ORIGIN,
            true,
            0,
            0,
            points.subList(0, 1),
            entities(points),
            travel(points)
        );
        var second = planner.build(
            2,
            DATE.plusDays(1),
            LocalTime.of(8, 0),
            LocalTime.of(17, 0),
            ORIGIN,
            true,
            0,
            0,
            points.subList(1, 2),
            entities(points),
            travel(points)
        );
        var allocation = new DraftAllocation(
            List.of(),
            Map.of(
                1,
                new ThemeHint("CULTURE_ART", List.of(points.get(0).placeId())),
                2,
                new ThemeHint("SEA_SCENERY", List.of(points.get(1).placeId()))
            ),
            "AI",
            null
        );
        var result = service.narrate(List.of(first, second), allocation);
        assertThat(result.source()).isEqualTo("AI_WITH_FALLBACK");
        assertThat(result.days())
            .extracting(d -> d.summarySource())
            .containsExactly("AI_GROUNDED_TEMPLATE", "RULE_BASED_FALLBACK");
    }
}
