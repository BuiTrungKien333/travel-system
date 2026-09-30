package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import lombok.Getter;

import java.util.*;

@Getter
public class DistanceMatrix {

    private final List<Place> places;

    // map cái uuid của từng phần tử trong places sang dạng index trong places (abc-xyz -> 2) để truy xuất cho nhanh
    // Map UUID -> Integer để lookup index nhanh từ PlaceId.
    private final Map<UUID, Integer> placeIndexMap;

    private final double[][] matrix;

    public DistanceMatrix(List<Place> places, double[][] matrix) {
        this.places = List.copyOf(places);
        this.matrix = matrix;

        Map<UUID, Integer> indexMap = new HashMap<>(places.size());
        for (int i = 0; i < places.size(); i++) {
            indexMap.put(places.get(i).getId(), i);
        }
        this.placeIndexMap = Map.copyOf(indexMap);
    }

    public int size() {
        return places.size();
    }

    public double getDistance(int i, int j) {
        return matrix[i][j];
    }

    public Place getPlace(int index) {
        return places.get(index);
    }

    public Optional<Integer> indexOf(UUID placeId) {
        if (placeId == null)
            return Optional.empty();

        return Optional.ofNullable(placeIndexMap.get(placeId));
    }

}
