package com.travel.planning.enums;

/** Haversine timing assumptions, not road-provider travel times. Motorized modes retain the existing 35 km/h model. */
public enum PlanningTravelMode {
    DRIVE(35.0), TWO_WHEELER(35.0), WALK(4.5);
    private final double speedKmPerHour;
    PlanningTravelMode(double speedKmPerHour) { this.speedKmPerHour = speedKmPerHour; }
    public double speedKmPerHour() { return speedKmPerHour; }
}
