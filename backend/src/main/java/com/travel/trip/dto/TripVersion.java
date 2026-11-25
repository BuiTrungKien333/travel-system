package com.travel.trip.dto;

import java.math.BigInteger;

/** Validate raw JSON before either Jackson generation can coerce a string/fraction into a lock counter. */
final class TripVersion {

    private TripVersion() {}

    static long parse(Object value) {
        if (value instanceof Integer number) return number.longValue();
        if (value instanceof Long number) return number;
        if (value instanceof BigInteger number && number.bitLength() <= 63) return number.longValue();
        throw new IllegalArgumentException("Trip version must be an integer");
    }
}
