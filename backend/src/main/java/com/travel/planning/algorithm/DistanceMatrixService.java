package com.travel.planning.algorithm;

import com.travel.place.entity.Place;
import com.travel.planning.algorithm.model.DistanceMatrix;

import java.util.List;

public interface DistanceMatrixService {

    /**
     * Tạo ma trận khoảng cách giữa các địa điểm trong danh sách
     *
     * @param places Danh sách các địa điểm
     * @return {@link DistanceMatrix} ma trận chi phí khoảng cách
     */
    DistanceMatrix calculateMatrix(List<Place> places);
}
