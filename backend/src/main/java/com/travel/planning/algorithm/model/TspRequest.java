package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TspRequest {

    @Builder.Default
    private List<Place> places = List.of();

    @Builder.Default
    private boolean roundTrip = true;

    private UUID startPlaceId;

    private UUID endPlaceId;
}
