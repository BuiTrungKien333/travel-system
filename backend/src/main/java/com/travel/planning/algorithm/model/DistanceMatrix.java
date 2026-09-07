package com.travel.planning.algorithm.model;

import com.travel.place.entity.Place;
import lombok.Getter;

import java.util.*;

@Getter
public class DistanceMatrix {

    private final List<Place> places;
    private final double[][] matrix;
    private final Map<UUID, Integer> placeIndexMap;

    public DistanceMatrix(List<Place> places, double[][] matrix) {
        this.places = Collections.unmodifiableList(new ArrayList<>(places));
        this.matrix = matrix;

        Map<UUID, Integer> indexMap = new HashMap<>(places.size());
        for (int i = 0; i < places.size(); i++) {
            indexMap.put(places.get(i).getId(), i);
        }
        this.placeIndexMap = Collections.unmodifiableMap(indexMap);
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
        if (placeId == null) return Optional.empty();
        return Optional.ofNullable(placeIndexMap.get(placeId));
    }
}
