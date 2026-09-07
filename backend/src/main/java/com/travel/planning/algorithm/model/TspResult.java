package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TspResult {

    @Builder.Default
    List<Place> orderedPlaces = Collections.emptyList();

    double totalDistanceMeters;

    long totalDurationSeconds;

    @Builder.Default
    List<RouteLeg> legs = Collections.emptyList();

    boolean roundTrip;
}
