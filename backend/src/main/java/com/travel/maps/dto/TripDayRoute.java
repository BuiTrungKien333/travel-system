package com.travel.maps.dto;

import java.util.List;
import java.util.UUID;

public record TripDayRoute(Status status, Route route) {
    public enum Status { READY, TOO_FEW_STOPS, MISSING_COORDINATES, TOO_MANY_STOPS, NOT_CONFIGURED, NO_ROUTE }

    public record Route(double distanceMeters, double durationSeconds, String encodedPolyline, List<Leg> legs) { }
    public record Leg(double distanceMeters, double durationSeconds) { }
    public record Response(Status status, Route route, List<UUID> placeIds, Leg startLeg, Leg returnLeg) { }
    public record Proposal(List<UUID> order, List<UUID> originalOrder, double distanceMeters, double durationSeconds) { }

    public static TripDayRoute unavailable(Status status) {
        return new TripDayRoute(status, null);
    }
}
