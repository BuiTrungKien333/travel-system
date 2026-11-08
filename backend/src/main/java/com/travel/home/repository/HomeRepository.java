package com.travel.home.repository;

import com.travel.home.dto.HomeDashboard.Footprint;
import com.travel.home.dto.HomeDashboard.Photo;
import com.travel.home.dto.HomeDashboard.Stop;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Bounded owner-scoped projections. Trip selection scans all eligible trips, never a list page.
 */
@Repository
@RequiredArgsConstructor
public class HomeRepository {

    private final JdbcTemplate jdbc;

    private static final String TODAY_STOPS = """
            FROM itinerary_items ii JOIN itineraries i ON i.id=ii.itinerary_id
            JOIN trip_places tp ON tp.trip_id=i.trip_id AND tp.place_id=ii.place_id
            JOIN places p ON p.id=ii.place_id
            WHERE i.id=(SELECT id FROM itineraries WHERE trip_id=? ORDER BY updated_at DESC,id ASC LIMIT 1)
              AND ii.date=? AND ii.removed_at IS NULL
            """;

    public Optional<UUID> selectedTrip(UUID owner, LocalDate today) {
        return jdbc.query("""
                WITH calendar AS (SELECT ?::date AS today), candidates AS (
                  SELECT t.id,t.created_at,t.start_date,c.today,
                    CASE WHEN t.status='COMPLETED' THEN 'RECAP'
                      WHEN t.start_date IS NULL OR t.end_date IS NULL OR t.end_date-t.start_date NOT BETWEEN 0 AND 364 THEN 'PLANNING'
                      WHEN c.today<t.start_date THEN 'PLANNING'
                      WHEN c.today>t.end_date THEN 'RECAP' ELSE 'LIVE' END AS mode
                  FROM trips t CROSS JOIN calendar c WHERE t.user_id=? AND t.deleted_at IS NULL AND t.status<>'CANCELLED'
                ) SELECT id FROM candidates ORDER BY
                    CASE WHEN mode='LIVE' THEN 0
                      WHEN mode='PLANNING' AND start_date>today AND start_date<=today+7 THEN 1
                      WHEN mode='PLANNING' THEN 2 ELSE 3 END, created_at DESC,id DESC LIMIT 1
                """, (row, index) -> row.getObject("id", UUID.class), today, owner).stream().findFirst();
    }

    public Footprint footprint(UUID owner, LocalDate today) {
        return jdbc.queryForObject("""
                SELECT count(DISTINCT p.province_id) AS provinces,count(DISTINCT p.id) AS places,
                  EXISTS(SELECT 1 FROM trips h WHERE h.user_id=? AND h.deleted_at IS NULL AND h.status<>'CANCELLED'
                    AND (h.status='COMPLETED' OR (h.start_date IS NOT NULL AND h.end_date>=h.start_date AND h.end_date<?))) AS history
                FROM memories m LEFT JOIN places p ON p.id=m.place_id AND (p.source='INTERNAL' OR p.owner_id=m.user_id)
                LEFT JOIN trips t ON t.id=m.trip_id
                WHERE m.user_id=? AND m.deleted_at IS NULL
                  AND (m.trip_id IS NULL OR (t.user_id=m.user_id AND t.deleted_at IS NULL))
                """, (r, i) -> new Footprint(r.getLong("provinces"), r.getLong("places"), r.getBoolean("history")), owner, today, owner);
    }

    public long todayCount(UUID trip, LocalDate today) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT count(*) " + TODAY_STOPS, Long.class, trip, today));
    }

    public Stop nextStop(UUID trip, LocalDate today) {
        return jdbc.query("""
                SELECT tp.id, p.id AS place_id,ii.id AS itinerary_item_id,p.name,p.address,p.latitude,p.longitude,p.category,
                       to_char(ii.arrival_at,'HH24:MI') AS time,ii.duration_minutes,ii.note
                """ + TODAY_STOPS + " AND NOT tp.visited ORDER BY ii.sequence,ii.id LIMIT 1",
                (r, i) -> new Stop(
                        r.getObject("id", UUID.class),
                        r.getObject("place_id", UUID.class),
                        r.getObject("itinerary_item_id", UUID.class),
                        r.getString("name"),
                        r.getString("address"),
                        r.getObject("latitude", Double.class),
                        r.getObject("longitude", Double.class),
                        r.getString("category"),
                        r.getString("time"),
                        r.getInt("duration_minutes"),
                        r.getString("note")
                ), trip, today).stream().findFirst().orElse(null);
    }

    public Integer firstEmptyDay(UUID trip) {
        return jdbc.query("""
                SELECT (d.date-t.start_date)+1 AS day FROM trip_days d JOIN trips t ON t.id=d.trip_id WHERE d.trip_id=?
                  AND d.date BETWEEN t.start_date AND t.end_date
                  AND NOT EXISTS(SELECT 1 FROM itinerary_items ii JOIN itineraries i ON i.id=ii.itinerary_id
                    WHERE i.id=(SELECT id FROM itineraries WHERE trip_id=d.trip_id ORDER BY updated_at DESC,id ASC LIMIT 1)
                      AND ii.date=d.date AND ii.removed_at IS NULL)
                ORDER BY d.date LIMIT 1
                """, (r, i) -> r.getInt("day"), trip).stream().findFirst().orElse(null);
    }

    public List<Photo> photos(UUID owner, UUID liveTrip, LocalDate today) {
        List<Object> args = new ArrayList<>();
        args.add(owner);
        String scope = "";
        if (liveTrip != null) {
            scope = " AND m.trip_id=? AND m.experienced_at>=? AND m.experienced_at<?";
            args.add(liveTrip);
            args.add(Timestamp.from(today.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()));
            args.add(Timestamp.from(today.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()));
        }
        return jdbc.query("""
                SELECT photo.id,m.id AS memory_id,m.trip_id,t.destination,p.name AS place_name,photo.url,m.note,m.experienced_at,
                       (m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS date,
                       to_char(m.experienced_at AT TIME ZONE 'Asia/Ho_Chi_Minh','HH24:MI') AS time
                FROM media photo JOIN memories m ON m.id=photo.memory_id
                LEFT JOIN trips t ON t.id=m.trip_id LEFT JOIN places p ON p.id=m.place_id AND (p.source='INTERNAL' OR p.owner_id=m.user_id)
                WHERE m.user_id=? AND m.deleted_at IS NULL AND photo.mime_type LIKE 'image/%'
                  AND (m.trip_id IS NULL OR (t.user_id=m.user_id AND t.deleted_at IS NULL))
                """ + scope + " ORDER BY m.experienced_at DESC NULLS LAST,m.id DESC,photo.display_order,photo.id LIMIT 6",
                (r, i) -> new Photo(
                        r.getObject("id", UUID.class),
                        r.getObject("memory_id", UUID.class),
                        r.getObject("trip_id", UUID.class),
                        r.getString("destination"),
                        r.getString("place_name"),
                        r.getString("url"),
                        r.getString("note"),
                        r.getObject("date", LocalDate.class),
                        r.getString("time"),
                        r.getTimestamp("experienced_at") == null ? null : r.getTimestamp("experienced_at").toInstant()
                ), args.toArray());
    }
}
