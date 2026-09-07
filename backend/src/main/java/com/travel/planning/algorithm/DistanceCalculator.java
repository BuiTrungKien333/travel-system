package com.travel.planning.algorithm;

public final class DistanceCalculator {

    // bán kính trung bình trái đất
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    // tốc độ di chuyển trung bình 35km/h
    public static final double DEFAULT_SPEED_KM_PER_HOUR = 35.0;

    private DistanceCalculator() {
        // Utility class, prevent instantiation
    }

    /**
     * Tính khoảng cách đường chim bay giữa 2 tọa độ GPS bằng công thức Haversine
     *
     * @param lat1 Vĩ độ điểm 1 (degrees)
     * @param lon1 Kinh độ điểm 1 (degrees)
     * @param lat2 Vĩ độ điểm 2 (degrees)
     * @param lon2 Kinh độ điểm 2 (degrees)
     * @return Khoảng cách tính bằng mét
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) {
            return 0.0;
        }

        // Chuyển đổi đơn vị (Degrees sang Radians)
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2.0) * Math.sin(deltaPhi / 2.0)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2.0) * Math.sin(deltaLambda / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Ước lượng thời gian di chuyển (giây) theo khoảng cách và vận tốc
     *
     * @param distanceMeters Khoảng cách (mét)
     * @param speedKmPerHour Vận tốc trung bình (km/h)
     * @return Thời gian di chuyển ước lượng tính bằng giây
     */
    public static long calculateDurationSeconds(double distanceMeters, double speedKmPerHour) {
        if (distanceMeters <= 0.0) {
            return 0L;
        }
        double speed = speedKmPerHour > 0 ? speedKmPerHour : DEFAULT_SPEED_KM_PER_HOUR;
        double speedMetersPerSec = (speed * 1000.0) / 3600.0;
        return Math.round(distanceMeters / speedMetersPerSec);
    }

    /**
     * Ước lượng thời gian di chuyển (giây) với vận tốc mặc định 35 km/h
     */
    public static long calculateDurationSeconds(double distanceMeters) {
        return calculateDurationSeconds(distanceMeters, DEFAULT_SPEED_KM_PER_HOUR);
    }
}
