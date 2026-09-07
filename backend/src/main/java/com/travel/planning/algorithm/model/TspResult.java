package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TspResult {

    @Builder.Default
    private List<Place> orderedPlaces = List.of();

    private double totalDistanceMeters;

    private long totalDurationSeconds;

    @Builder.Default
    private List<RouteLeg> legs = List.of();

    private boolean roundTrip;

}
