package com.travel.planning.draft.model;

public final class DraftLimits {

    public static final int MAX_DAYS = 3;
    // The routing matrix accepts 12 points, including the trip origin.
    public static final int CANDIDATE_LIMIT = 11;
    public static final int MAX_STOPS_PER_DAY = 5;

    private DraftLimits() {}
}
