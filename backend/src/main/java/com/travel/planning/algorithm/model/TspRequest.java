package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TspRequest {

    @Builder.Default
    List<Place> places = Collections.emptyList();

    @Builder.Default
    boolean roundTrip = true;

    UUID startPlaceId;

    UUID endPlaceId;
}
