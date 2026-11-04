package com.travel.planning.draft.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validated DB identifiers and optional theme hints from the same allocation call.
 */
public record DraftAllocation(List<List<UUID>> days, Map<Integer, ThemeHint> themes, String source, String reason) {
    public record ThemeHint(String theme, List<UUID> highlights) {
    }
}
