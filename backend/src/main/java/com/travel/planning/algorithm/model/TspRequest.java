package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import lombok.*;

import java.util.List;
import java.util.UUID;

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

    /** Zero-based visit positions that cannot be moved by the solver. */
    @Builder.Default
    private java.util.Map<Integer, UUID> fixedPositions = java.util.Map.of();
    @Builder.Default
    private com.travel.planning.enums.PlanningTravelMode travelMode = com.travel.planning.enums.PlanningTravelMode.DRIVE;

}
