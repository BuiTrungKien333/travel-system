package com.travel.trip.repository;

import com.travel.trip.dto.CreateTripRequest;
import com.travel.trip.dto.TripResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/** Bounded read projections and PostgreSQL atomic create-key insertion, without loading JPA graphs. */
@Repository
@RequiredArgsConstructor
public class TripCrudRepository {
    private final JdbcTemplate jdbc;
    public record Row(UUID id, String title, String destination, UUID provinceId,
            LocalDate start, LocalDate end, String zone, String status, long version,
            java.time.Instant created, java.time.Instant updated, Long budget,
            String preferences, String accommodation, String notes, String hash,
            boolean deleted, long stopCount, long unscheduledCount, long memoryCount) { }

    private static final String PROJECTION = """
        SELECT t.*,
          (SELECT count(*) FROM trip_places tp WHERE tp.trip_id=t.id) AS stop_count,
          (SELECT count(*) FROM trip_places tp WHERE tp.trip_id=t.id AND NOT EXISTS
            (SELECT 1 FROM itineraries i JOIN itinerary_items ii ON ii.itinerary_id=i.id
             WHERE i.trip_id=t.id AND ii.place_id=tp.place_id)) AS unscheduled_count,
          (SELECT count(*) FROM memories m WHERE m.trip_id=t.id AND m.deleted_at IS NULL) AS memory_count
        FROM trips t
        """;

    private Row map(ResultSet rs, int ignored) throws SQLException {
        return new Row(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("destination"),
            rs.getObject("destination_province_id", UUID.class), rs.getObject("start_date", LocalDate.class),
            rs.getObject("end_date", LocalDate.class), rs.getString("time_zone"), rs.getString("status"),
            rs.getLong("version"), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
            rs.getObject("budget_amount", Long.class), rs.getString("preferences"), rs.getString("accommodation"),
            rs.getString("notes"), rs.getString("create_request_hash"), rs.getTimestamp("deleted_at") != null,
            rs.getLong("stop_count"), rs.getLong("unscheduled_count"), rs.getLong("memory_count"));
    }
    public Optional<Row> find(UUID owner, UUID id) {
        return jdbc.query(PROJECTION + " WHERE t.user_id=? AND t.id=? AND t.deleted_at IS NULL", this::map, owner, id).stream().findFirst();
    }
    public Optional<Row> byCreateKey(UUID owner, UUID key) {
        return jdbc.query(PROJECTION + " WHERE t.user_id=? AND t.client_request_id=?", this::map, owner, key).stream().findFirst();
    }
    public boolean referencesExist(CreateTripRequest r) {
        if (r.destination().provinceId() != null && !Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM provinces WHERE id=?)", Boolean.class, r.destination().provinceId()))) return false;
        if (r.initialPlaceIds().isEmpty()) return true;
        String marks = String.join(",", Collections.nCopies(r.initialPlaceIds().size(), "?"));
        Long count = jdbc.queryForObject("SELECT count(*) FROM places WHERE id IN (" + marks + ")", Long.class, r.initialPlaceIds().toArray());
        return count != null && count == r.initialPlaceIds().size();
    }
    public boolean insert(UUID id, UUID owner, CreateTripRequest r, String title, String hash, String preferences) {
        return jdbc.update("""
            INSERT INTO trips(id,user_id,name,destination,start_date,end_date,preferences,status,time_zone,
              destination_province_id,budget_amount,budget_currency,accommodation,notes,client_request_id,create_request_hash)
            VALUES (?,?,?,?,?,?,?,'PLANNING',?,?,?,?,?,?,?,?)
            ON CONFLICT (user_id,client_request_id) DO NOTHING
            """, id, owner, title, r.destination().name(), r.startDate(), r.endDate(), preferences, r.timeZone(),
            r.destination().provinceId(), r.budget() == null ? null : r.budget().amount().longValueExact(),
            r.budget() == null ? null : r.budget().currency(), r.accommodation(), r.notes(), r.clientRequestId(), hash) == 1;
    }
    public void initialize(UUID id, CreateTripRequest r) {
        jdbc.update("INSERT INTO itineraries(trip_id) VALUES (?)", id);
        List<LocalDate> dates = r.startDate().datesUntil(r.endDate().plusDays(1)).toList();
        jdbc.batchUpdate("INSERT INTO trip_days(trip_id,date) VALUES (?,?)", dates, 100,
                (ps, day) -> { ps.setObject(1,id); ps.setObject(2,day); });
        jdbc.batchUpdate("INSERT INTO trip_places(trip_id,place_id) VALUES (?,?)", r.initialPlaceIds(), 100,
                (ps, place) -> { ps.setObject(1,id); ps.setObject(2,place); });
    }
    public List<TripResponses.Day> days(UUID id, LocalDate start) {
        return jdbc.query("SELECT id,date FROM trip_days WHERE trip_id=? ORDER BY date", (rs,n) -> {
            LocalDate day = rs.getObject("date",LocalDate.class);
            return new TripResponses.Day(rs.getObject("id",UUID.class),day,
                start == null ? n + 1 : Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(start,day) + 1));
        },id);
    }
    public record Filter(String clause, List<Object> values) { }
    public Filter filter(UUID owner, String mode, String q, LocalDate today) {
        List<Object> args = new ArrayList<>(); args.add(owner);
        String sql = " WHERE t.user_id=? AND t.deleted_at IS NULL";
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_") + "%";
            sql += " AND (lower(t.name) LIKE ? ESCAPE E'\\\\' OR lower(t.destination) LIKE ? ESCAPE E'\\\\')";
            args.add(pattern); args.add(pattern);
        }
        if (mode != null) {
            sql += """
                 AND (CASE WHEN t.status='CANCELLED' THEN 'CANCELLED'
                  WHEN t.status='COMPLETED' THEN 'RECAP'
                  WHEN t.start_date IS NULL OR t.end_date IS NULL OR t.end_date<t.start_date THEN 'PLANNING'
                  WHEN ? < t.start_date THEN 'PLANNING' WHEN ? > t.end_date THEN 'RECAP' ELSE 'LIVE' END)=?
                """;
            args.add(today); args.add(today); args.add(mode);
        }
        return new Filter(sql, args);
    }
    public long count(Filter f) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT count(*) FROM trips t" + f.clause(), Long.class, f.values().toArray()));
    }
    public List<Row> list(Filter f, String order, int size, long offset) {
        List<Object> args = new ArrayList<>(f.values()); args.add(size); args.add(offset);
        return jdbc.query(PROJECTION + f.clause() + " ORDER BY " + order + " LIMIT ? OFFSET ?", this::map, args.toArray());
    }
}
