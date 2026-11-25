package com.travel.planning.algorithm;

import com.travel.place.entity.Place;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j(topic = "DISTANCE_MATRIX_SERVICE")
public class HaversineDistanceMatrixService {

    public double[][] calculateMatrix(List<Place> places) {
        if (places == null || places.isEmpty()) {
            return new double[0][0];
        }

        int n = places.size();
        log.debug("[DISTANCE_MATRIX] Calculating Haversine distance matrix for {} places", n);

        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            Place p1 = places.get(i);
            for (int j = i + 1; j < n; j++) {
                Place p2 = places.get(j);
                double distance = DistanceCalculator.calculateDistanceMeters(
                    p1.getLatitude(),
                    p1.getLongitude(),
                    p2.getLatitude(),
                    p2.getLongitude()
                );
                matrix[i][j] = distance;
                matrix[j][i] = distance; // Ma trận đối xứng cho khoảng cách chim bay
            }
        }

        return matrix;
    }
}
