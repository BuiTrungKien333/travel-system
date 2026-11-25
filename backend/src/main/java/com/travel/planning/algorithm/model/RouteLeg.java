package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteLeg {

    private Place fromPlace;
    private Place toPlace;
    private double distanceMeters;
    private long durationSeconds;
}
