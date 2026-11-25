package com.travel.weather.repository;

import com.travel.weather.model.TripWeather.Location;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TripWeatherRepository {

    private final JdbcTemplate jdbc;

    public record TripDates(LocalDate start, LocalDate end, UUID provinceId) {}

    public Optional<TripDates> trip(UUID owner, UUID id) {
        return jdbc
            .query(
                """
                SELECT start_date,end_date,destination_province_id FROM trips
                WHERE id=? AND user_id=? AND deleted_at IS NULL
                """,
                (r, i) ->
                    new TripDates(
                        r.getObject(1, LocalDate.class),
                        r.getObject(2, LocalDate.class),
                        r.getObject(3, UUID.class)
                    ),
                id,
                owner
            )
            .stream()
            .findFirst();
    }

    public Optional<Location> location(UUID owner, UUID trip, UUID province, LocalDate date) {
        return jdbc
            .query(
                """
                SELECT * FROM (
                  SELECT p.id,p.name,p.latitude,p.longitude,'ITINERARY' AS basis,0 AS priority,ii.sequence AS sequence
                  FROM itinerary_items ii JOIN itineraries i ON i.id=ii.itinerary_id
                  JOIN trip_places tp ON tp.trip_id=i.trip_id AND tp.place_id=ii.place_id JOIN places p ON p.id=tp.place_id
                  WHERE i.id=(SELECT id FROM itineraries WHERE trip_id=? ORDER BY updated_at DESC,id ASC LIMIT 1)
                    AND ii.date=? AND ii.removed_at IS NULL AND NOT tp.visited AND (p.source='INTERNAL' OR p.owner_id=?)
                  UNION ALL
                  SELECT p.id,p.name,p.latitude,p.longitude,'SAVED_PLACE',1,0 FROM trip_places tp JOIN places p ON p.id=tp.place_id
                  WHERE tp.trip_id=? AND (p.source='INTERNAL' OR p.owner_id=?)
                  UNION ALL
                  SELECT p.id,p.name,p.latitude,p.longitude,'DESTINATION_CATALOG',2,0 FROM places p
                  JOIN place_retrieval_profiles pr ON pr.place_id=p.id
                  WHERE p.province_id=? AND p.source='INTERNAL' AND pr.approved=true
                ) candidates WHERE latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180
                ORDER BY priority,sequence,id LIMIT 1
                """,
                (r, i) ->
                    new Location(
                        r.getObject("id", UUID.class),
                        r.getString("name"),
                        r.getDouble("latitude"),
                        r.getDouble("longitude"),
                        r.getString("basis")
                    ),
                trip,
                date,
                owner,
                trip,
                owner,
                province
            )
            .stream()
            .findFirst();
    }
}
