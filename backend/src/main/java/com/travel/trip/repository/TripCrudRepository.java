package com.travel.trip.repository;

import com.travel.trip.dto.CreateTripRequest;
import com.travel.trip.dto.TripResponses;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Bounded read projections and PostgreSQL atomic create-key insertion, without loading JPA graphs.
 */
@Repository
@RequiredArgsConstructor
public class TripCrudRepository {

    private final JdbcTemplate jdbc;

    public record Row(
        UUID id,
        String title,
        String destination,
        UUID provinceId,
        LocalDate start,
        LocalDate end,
        String zone,
        String status,
        long version,
        Instant created,
        Instant updated,
        Long budget,
        String preferences,
        String accommodation,
        String notes,
        String hash,
        boolean deleted,
        long stopCount,
        long unscheduledCount,
        long memoryCount,
        String coverImage
    ) {}

    public record Metadata(
        String title,
        String destination,
        UUID provinceId,
        LocalDate start,
        LocalDate end,
        Long budget,
        String preferences,
        String accommodation,
        String notes
    ) {}

    public record Filter(String clause, List<Object> values) {}

    private static final String PROJECTION = """
    SELECT t.*,
      (SELECT count(*) FROM trip_places tp WHERE tp.trip_id=t.id) AS stop_count,
      (SELECT count(*) FROM trip_places tp WHERE tp.trip_id=t.id AND NOT EXISTS
        (SELECT 1 FROM itineraries i JOIN itinerary_items ii ON ii.itinerary_id=i.id
         WHERE i.trip_id=t.id AND ii.place_id=tp.place_id AND ii.removed_at IS NULL)) AS unscheduled_count,
      (SELECT count(*) FROM memories m WHERE m.trip_id=t.id AND m.user_id=t.user_id AND m.deleted_at IS NULL) AS memory_count
    FROM trips t
    """;

    private Row map(ResultSet rs, int ignored) throws SQLException {
        return new Row(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getString("destination"),
            rs.getObject("destination_province_id", UUID.class),
            rs.getObject("start_date", LocalDate.class),
            rs.getObject("end_date", LocalDate.class),
            rs.getString("time_zone"),
            rs.getString("status"),
            rs.getLong("version"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant(),
            rs.getObject("budget_amount", Long.class),
            rs.getString("preferences"),
            rs.getString("accommodation"),
            rs.getString("notes"),
            rs.getString("create_request_hash"),
            rs.getTimestamp("deleted_at") != null,
            rs.getLong("stop_count"),
            rs.getLong("unscheduled_count"),
            rs.getLong("memory_count"),
            rs.getString("cover_image")
        );
    }

    public Optional<Row> find(UUID owner, UUID id) {
        return jdbc
            .query(PROJECTION + " WHERE t.user_id=? AND t.id=? AND t.deleted_at IS NULL", this::map, owner, id)
            .stream()
            .findFirst();
    }

    public Optional<Row> byCreateKey(UUID owner, UUID key) {
        return jdbc
            .query(PROJECTION + " WHERE t.user_id=? AND t.client_request_id=?", this::map, owner, key)
            .stream()
            .findFirst();
    }

    public Optional<Row> lockOwned(UUID owner, UUID id) {
        return jdbc
            .query(PROJECTION + " WHERE t.user_id=? AND t.id=? FOR UPDATE OF t", this::map, owner, id)
            .stream()
            .findFirst();
    }

    public boolean provinceExists(UUID provinceId) {
        return (
            provinceId == null ||
            Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM provinces WHERE id=?)", Boolean.class, provinceId)
            )
        );
    }

    public boolean update(UUID id, long version, Metadata value, Instant now) {
        return (
            jdbc.update(
                """
                UPDATE trips SET name=?, destination=?, destination_province_id=?, start_date=?, end_date=?,
                    budget_amount=?, budget_currency=?, preferences=?, accommodation=?, notes=?, version=version+1, updated_at=?
                WHERE id=? AND version=? AND deleted_at IS NULL
                """,
                value.title(),
                value.destination(),
                value.provinceId(),
                value.start(),
                value.end(),
                value.budget(),
                value.budget() == null ? null : "VND",
                value.preferences(),
                value.accommodation(),
                value.notes(),
                java.sql.Timestamp.from(now),
                id,
                version
            ) == 1
        );
    }

    public long movedToPoolCount(UUID id, LocalDate start, LocalDate end) {
        return Objects.requireNonNull(
            jdbc.queryForObject(
                """
                SELECT count(DISTINCT ii.place_id) FROM itinerary_items ii JOIN itineraries i ON i.id=ii.itinerary_id
                WHERE i.trip_id=? AND ii.removed_at IS NULL AND (ii.date<? OR ii.date>?) AND NOT EXISTS (
                    SELECT 1 FROM itinerary_items kept JOIN itineraries ki ON ki.id=kept.itinerary_id
                    WHERE ki.trip_id=i.trip_id AND kept.place_id=ii.place_id AND kept.removed_at IS NULL AND kept.date BETWEEN ? AND ?)
                """,
                Long.class,
                id,
                start,
                end,
                start,
                end
            )
        );
    }

    public void changeDates(UUID id, LocalDate start, LocalDate end, Instant now) {
        // Preserve legacy memberships missing from trip_places; never overwrite its canonical notes/duration/visited.
        jdbc.update(
            """
            INSERT INTO trip_places(trip_id,place_id)
            SELECT DISTINCT i.trip_id, ii.place_id FROM itineraries i JOIN itinerary_items ii ON ii.itinerary_id=i.id
            WHERE i.trip_id=? AND ii.removed_at IS NULL AND (ii.date<? OR ii.date>?)
            ON CONFLICT (trip_id,place_id) DO NOTHING
            """,
            id,
            start,
            end
        );

        jdbc.update(
            """
            UPDATE itinerary_items ii SET removed_at=?, updated_at=? FROM itineraries i
            WHERE i.id=ii.itinerary_id AND i.trip_id=? AND ii.removed_at IS NULL AND (ii.date<? OR ii.date>?)
            """,
            java.sql.Timestamp.from(now),
            java.sql.Timestamp.from(now),
            id,
            start,
            end
        );

        jdbc.update("DELETE FROM trip_days WHERE trip_id=? AND (date<? OR date>?)", id, start, end);

        var dates = start.datesUntil(end.plusDays(1)).toList();
        jdbc.batchUpdate(
            "INSERT INTO trip_days(trip_id,date) VALUES (?,?) ON CONFLICT (trip_id,date) DO NOTHING",
            dates,
            100,
            (ps, date) -> {
                ps.setObject(1, id);
                ps.setObject(2, date);
            }
        );
    }

    public void softDelete(UUID id, Instant now) {
        var timestamp = java.sql.Timestamp.from(now);
        jdbc.update(
            "UPDATE trips SET deleted_at=?, updated_at=?, version=version+1 WHERE id=? AND deleted_at IS NULL",
            timestamp,
            timestamp,
            id
        );
        jdbc.update(
            """
            UPDATE memories SET deleted_at=?, updated_at=?
            WHERE trip_id=? AND user_id=(SELECT user_id FROM trips WHERE id=?) AND deleted_at IS NULL
            """,
            timestamp,
            timestamp,
            id,
            id
        );
    }

    public boolean complete(UUID id, long version, Instant now) {
        return (
            jdbc.update(
                """
                UPDATE trips SET status='COMPLETED', version=version+1, updated_at=?
                WHERE id=? AND version=? AND deleted_at IS NULL
                """,
                java.sql.Timestamp.from(now),
                id,
                version
            ) == 1
        );
    }

    public boolean referencesExist(UUID owner, CreateTripRequest r) {
        if (
            r.destination().provinceId() != null &&
            !Boolean.TRUE.equals(
                jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM provinces WHERE id=?)",
                    Boolean.class,
                    r.destination().provinceId()
                )
            )
        ) {
            return false;
        }
        if (r.initialPlaceIds().isEmpty()) {
            return true;
        }
        String marks = String.join(",", Collections.nCopies(r.initialPlaceIds().size(), "?"));
        List<Object> arguments = new ArrayList<>(r.initialPlaceIds());
        arguments.add(owner);
        Long count = jdbc.queryForObject(
            "SELECT count(*) FROM places WHERE id IN (" + marks + ") AND (source='INTERNAL' OR owner_id=?)",
            Long.class,
            arguments.toArray()
        );
        return count != null && count == r.initialPlaceIds().size();
    }

    public boolean insert(UUID id, UUID owner, CreateTripRequest r, String title, String hash, String preferences) {
        return (
            jdbc.update(
                """
                INSERT INTO trips(id,user_id,name,destination,start_date,end_date,preferences,status,time_zone,
                  destination_province_id,budget_amount,budget_currency,accommodation,notes,client_request_id,create_request_hash)
                VALUES (?,?,?,?,?,?,?,'PLANNING',?,?,?,?,?,?,?,?)
                ON CONFLICT (user_id,client_request_id) DO NOTHING
                """,
                id,
                owner,
                title,
                r.destination().name(),
                r.startDate(),
                r.endDate(),
                preferences,
                r.timeZone(),
                r.destination().provinceId(),
                r.budget() == null ? null : r.budget().amount().longValueExact(),
                r.budget() == null ? null : r.budget().currency(),
                r.accommodation(),
                r.notes(),
                r.clientRequestId(),
                hash
            ) == 1
        );
    }

    public void initialize(UUID id, CreateTripRequest r) {
        jdbc.update("INSERT INTO itineraries(trip_id) VALUES (?)", id);
        List<LocalDate> dates = r.startDate().datesUntil(r.endDate().plusDays(1)).toList();
        jdbc.batchUpdate("INSERT INTO trip_days(trip_id,date) VALUES (?,?)", dates, 100, (ps, day) -> {
            ps.setObject(1, id);
            ps.setObject(2, day);
        });
        jdbc.batchUpdate(
            "INSERT INTO trip_places(trip_id,place_id) VALUES (?,?)",
            r.initialPlaceIds(),
            100,
            (ps, place) -> {
                ps.setObject(1, id);
                ps.setObject(2, place);
            }
        );
    }

    public List<TripResponses.Day> days(UUID id, LocalDate start) {
        return jdbc.query(
            "SELECT id,date FROM trip_days WHERE trip_id=? ORDER BY date",
            (rs, n) -> {
                LocalDate day = rs.getObject("date", LocalDate.class);
                return new TripResponses.Day(
                    rs.getObject("id", UUID.class),
                    day,
                    start == null ? n + 1 : Math.toIntExact(ChronoUnit.DAYS.between(start, day) + 1)
                );
            },
            id
        );
    }

    public Filter filter(UUID owner, String mode, String q, LocalDate today) {
        List<Object> args = new ArrayList<>();
        args.add(owner);
        String sql = " WHERE t.user_id=? AND t.deleted_at IS NULL";
        if (q != null && !q.isBlank()) {
            String pattern =
                "%" +
                q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") +
                "%";
            sql += " AND (lower(t.name) LIKE ? ESCAPE E'\\\\' OR lower(t.destination) LIKE ? ESCAPE E'\\\\')";
            args.add(pattern);
            args.add(pattern);
        }
        if (mode != null) {
            sql += """
             AND (CASE WHEN t.status='CANCELLED' THEN 'CANCELLED'
              WHEN t.status='COMPLETED' THEN 'RECAP'
              WHEN t.start_date IS NULL OR t.end_date IS NULL OR t.end_date<t.start_date THEN 'PLANNING'
              WHEN ? < t.start_date THEN 'PLANNING' WHEN ? > t.end_date THEN 'RECAP' ELSE 'LIVE' END)=?
            """;
            args.add(today);
            args.add(today);
            args.add(mode);
        }
        return new Filter(sql, args);
    }

    public long count(Filter f) {
        return Objects.requireNonNull(
            jdbc.queryForObject("SELECT count(*) FROM trips t" + f.clause(), Long.class, f.values().toArray())
        );
    }

    public List<Row> list(Filter f, String order, int size, long offset) {
        List<Object> args = new ArrayList<>(f.values());
        args.add(size);
        args.add(offset);
        return jdbc.query(
            PROJECTION + f.clause() + " ORDER BY " + order + " LIMIT ? OFFSET ?",
            this::map,
            args.toArray()
        );
    }
}
