package com.travel.maps.repository;

import com.travel.itinerary.dto.DaySettings;
import com.travel.itinerary.dto.DaySettings.TravelMode;
import com.travel.maps.dto.RoutePoint;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TripRouteRepository {

    private final JdbcTemplate jdbc;

    public record Dates(LocalDate start, LocalDate end, String status) {}

    public record Waypoint(UUID placeId, RoutePoint point, boolean visited) {}

    public Optional<Dates> ownedDates(UUID tripId, UUID owner) {
        return jdbc
            .query(
                """
                SELECT start_date, end_date, status FROM trips WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """,
                (row, index) ->
                    new Dates(row.getObject(1, LocalDate.class), row.getObject(2, LocalDate.class), row.getString(3)),
                tripId,
                owner
            )
            .stream()
            .findFirst();
    }

    public List<Waypoint> orderedPoints(UUID tripId, UUID owner, LocalDate date) {
        return jdbc.query(
            """
            SELECT p.id, p.latitude, p.longitude, saved.visited FROM itinerary_items s
            JOIN places p ON p.id = s.place_id
            JOIN trip_places saved ON saved.trip_id = ? AND saved.place_id = s.place_id
            JOIN trips t ON t.id = saved.trip_id AND t.user_id = ? AND t.deleted_at IS NULL
            WHERE s.itinerary_id = (SELECT id FROM itineraries WHERE trip_id = ? ORDER BY updated_at DESC, id ASC LIMIT 1)
              AND s.date = ? AND s.removed_at IS NULL AND (p.source = 'INTERNAL' OR p.owner_id = ?)
            ORDER BY s.sequence, s.id LIMIT 101
            """,
            (row, index) ->
                new Waypoint(
                    row.getObject(1, UUID.class),
                    new RoutePoint(null, row.getObject(2, Double.class), row.getObject(3, Double.class)),
                    row.getBoolean(4)
                ),
            tripId,
            owner,
            tripId,
            date,
            owner
        );
    }

    public Optional<DaySettings> settings(UUID tripId, UUID owner, LocalDate date) {
        return jdbc
            .query(
                """
                SELECT d.origin_name, d.origin_address, d.origin_latitude, d.origin_longitude,
                       d.departure_time, d.return_to_start, d.travel_mode
                FROM itinerary_day_settings d
                JOIN itineraries i ON i.id = d.itinerary_id
                JOIN trips t ON t.id = i.trip_id AND t.user_id = ? AND t.deleted_at IS NULL
                WHERE i.id = (SELECT id FROM itineraries WHERE trip_id = ? ORDER BY updated_at DESC, id ASC LIMIT 1)
                  AND d.day_date = ?
                """,
                (row, index) ->
                    new DaySettings(
                        row.getString(1) == null
                            ? null
                            : new DaySettings.Origin(
                                  row.getString(1),
                                  row.getString(2),
                                  row.getObject(3, Double.class),
                                  row.getObject(4, Double.class)
                              ),
                        row.getObject(5, LocalTime.class),
                        row.getBoolean(6),
                        TravelMode.valueOf(row.getString(7))
                    ),
                owner,
                tripId,
                date
            )
            .stream()
            .findFirst();
    }
}
