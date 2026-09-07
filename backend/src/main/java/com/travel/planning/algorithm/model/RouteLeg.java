package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RouteLeg {

    Place fromPlace;
    Place toPlace;
    double distanceMeters;
    long durationSeconds;
}
