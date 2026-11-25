package com.travel.admin;

import com.travel.common.response.ApiResponse;
import com.travel.common.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.time.*;
import java.util.*;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminOverviewService {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    @Value("${application.rag.embedding-model:gemini-embedding-001}")
    private String model;

    public record Point(String name, long value) {
    }

    public record Trend(String name, long users, long trips) {
    }

    public record Knowledge(UUID id, String name, String province, List<String> tags, String sourceUrl,
                            String verificationScope, boolean approved, int version, String indexStatus) {
    }

    public record KnowledgePage(List<Knowledge> content, int page, int size, long totalElements, String model,
                                boolean indexAvailable) {
    }

    public record Overview(Instant asOf, int days, long users, long newUsers, long newTrips, long drafts,
                           long pendingPlaces, long places, long trips, long checkIns, List<Trend> trend, List<Point> tripStatuses, List<Point> topPlaces,
                           List<Point> topProvinces, List<Point> tags, List<Point> rag) {
    }

    private static final String ELIGIBLE = "p.source='INTERNAL' AND r.approved AND r.archived_at IS NULL";
    private static final String HASH = "encode(sha256(convert_to(p.name || E'\\n' || r.description || E'\\n' || array_to_string(r.tags, ', '), 'UTF8')), 'hex')";

    private boolean hasIndex() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('place_embeddings') IS NOT NULL", Boolean.class));
    }

    private List<Point> points(String sql, Object... args) {
        return jdbc.query(sql, (rs, n) -> new Point(rs.getString(1), rs.getLong(2)), args);
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:system:read')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Overview overview(int days) {
        if (!Set.of(7, 30, 90).contains(days)) throw new AppException(ErrorCode.VALIDATION_ERROR);
        Instant now = clock.instant();
        LocalDate today = now.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        Instant since = today.minusDays(days - 1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        var trend = jdbc.query("""
                WITH days AS (SELECT generate_series(?::date,?::date,'1 day')::date AS d),
                u AS (SELECT (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date d,count(*) n FROM users WHERE deleted_at IS NULL GROUP BY 1),
                t AS (SELECT (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date d,count(*) n FROM trips WHERE deleted_at IS NULL GROUP BY 1)
                SELECT days.d,coalesce(u.n,0),coalesce(t.n,0) FROM days LEFT JOIN u USING(d) LEFT JOIN t USING(d) ORDER BY days.d
                """, (rs, n) -> new Trend(rs.getString(1), rs.getLong(2), rs.getLong(3)), today.minusDays(days - 1), today);
        var rag = knowledge(0, 100).ragSummary;
        return new Overview(now, days, count("SELECT count(*) FROM users WHERE deleted_at IS NULL"),
                count("SELECT count(*) FROM users WHERE deleted_at IS NULL AND created_at>=? AND created_at<=?", java.sql.Timestamp.from(since), java.sql.Timestamp.from(now)),
                count("SELECT count(*) FROM trips WHERE deleted_at IS NULL AND created_at>=? AND created_at<=?", java.sql.Timestamp.from(since), java.sql.Timestamp.from(now)),
                count("SELECT count(*) FROM trip_planning_drafts WHERE created_at>=? AND created_at<=?", java.sql.Timestamp.from(since), java.sql.Timestamp.from(now)),
                count("SELECT count(*) FROM place_retrieval_profiles r JOIN places p ON p.id=r.place_id WHERE p.source='INTERNAL' AND r.archived_at IS NULL AND NOT r.approved"),
                count("SELECT count(*) FROM places p JOIN place_retrieval_profiles r ON r.place_id=p.id WHERE p.source='INTERNAL' AND r.archived_at IS NULL"),
                count("SELECT count(*) FROM trips WHERE deleted_at IS NULL"),
                count("SELECT count(*) FROM trip_places tp JOIN trips t ON t.id=tp.trip_id WHERE tp.visited AND t.deleted_at IS NULL"), trend,
                points("SELECT status,count(*) FROM trips WHERE deleted_at IS NULL GROUP BY status ORDER BY count(*) DESC"),
                points("SELECT p.name,count(DISTINCT tp.trip_id) FROM trip_places tp JOIN places p ON p.id=tp.place_id JOIN trips t ON t.id=tp.trip_id WHERE p.source='INTERNAL' AND t.deleted_at IS NULL AND t.created_at>=? AND t.created_at<=? GROUP BY p.name ORDER BY 2 DESC,p.name LIMIT 10", java.sql.Timestamp.from(since), java.sql.Timestamp.from(now)),
                points("SELECT province.name,count(*) FROM trips t JOIN provinces province ON province.id=t.destination_province_id WHERE t.deleted_at IS NULL AND t.created_at>=? AND t.created_at<=? GROUP BY province.id,province.name ORDER BY 2 DESC,province.name LIMIT 10", java.sql.Timestamp.from(since), java.sql.Timestamp.from(now)),
                points("SELECT tag,count(DISTINCT p.id) FROM places p JOIN place_retrieval_profiles r ON r.place_id=p.id CROSS JOIN LATERAL unnest(r.tags) tag WHERE p.source='INTERNAL' GROUP BY tag ORDER BY 2 DESC,tag LIMIT 14"), rag);
    }

    public record KnowledgeResult(KnowledgePage page, List<Point> ragSummary) {
    }

    private KnowledgeResult knowledge(int page, int size) {
        return knowledge(page, size, null, null);
    }

    private KnowledgeResult knowledge(int page, int size, String provinceCode, String indexStatus) {
        if (provinceCode != null && !provinceCode.isBlank() && !provinceCode.matches("\\d{2}"))
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        if (indexStatus != null && !Set.of("CURRENT", "STALE", "MISSING", "OUT_OF_SCOPE").contains(indexStatus))
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        boolean available = hasIndex();
        String join = " FROM places p JOIN place_retrieval_profiles r ON r.place_id=p.id LEFT JOIN provinces province ON province.id=p.province_id ";
        String state = "CASE WHEN NOT COALESCE((" + ELIGIBLE + "),false) THEN 'OUT_OF_SCOPE' " + (available ? "WHEN e.place_id IS NULL THEN 'MISSING' WHEN e.content_hash=" + HASH + " THEN 'CURRENT' ELSE 'STALE' END" : "ELSE 'MISSING' END");
        if (available) join += "LEFT JOIN place_embeddings e ON e.place_id=p.id AND e.model=? ";
        String where = " WHERE p.source='INTERNAL' AND r.archived_at IS NULL";
        var args = new ArrayList<Object>();
        if (available) args.add(model);
        if (provinceCode != null && !provinceCode.isBlank()) {
            where += " AND province.code=?";
            args.add(provinceCode);
        }
        if (indexStatus != null) {
            where += " AND (" + state + ")=?";
            args.add(indexStatus);
        }
        String base = "SELECT p.id,p.name,province.name province,r.*, " + state + " index_status" + join + where;
        var pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        var content = jdbc.query(base + " ORDER BY p.name,p.id LIMIT ? OFFSET ?", (rs, n) -> new Knowledge(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("province"), Arrays.asList((String[]) rs.getArray("tags").getArray()), rs.getString("source_url"), rs.getString("verification_scope"), rs.getBoolean("approved"), rs.getInt("content_version"), rs.getString("index_status")), pageArgs.toArray());
        var summaryArgs = available ? new Object[]{model} : new Object[]{};
        var summary = points("SELECT state,count(*) FROM (SELECT " + state + " state" + join + " WHERE p.source='INTERNAL' AND r.archived_at IS NULL) s GROUP BY state ORDER BY state", summaryArgs);
        Long total = jdbc.queryForObject("SELECT count(*) FROM (" + base + ") knowledge", Long.class, args.toArray());
        return new KnowledgeResult(new KnowledgePage(content, page, size, total == null ? 0 : total, model, available), summary);
    }

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:catalog')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public KnowledgePage list(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new AppException(ErrorCode.VALIDATION_ERROR);
        return knowledge(page, size).page();
    }

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:catalog')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public KnowledgePage list(int page, int size, String provinceCode, String indexStatus) {
        if (page < 0 || size < 1 || size > 100) throw new AppException(ErrorCode.VALIDATION_ERROR);
        return knowledge(page, size, provinceCode, indexStatus).page();
    }
}
