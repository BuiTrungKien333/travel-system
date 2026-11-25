package com.travel.place.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.place.enums.PlaceType;
import com.travel.security.util.SecurityUtils;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:catalog')")
public class AdminCatalogService {

    private static final String FROM =
        " FROM places p JOIN place_retrieval_profiles r ON r.place_id=p.id " +
        "LEFT JOIN provinces province ON province.id=p.province_id WHERE p.source='INTERNAL'";
    private static final java.util.regex.Pattern TAG = java.util.regex.Pattern.compile("[A-Z][A-Z0-9_]{1,29}");

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public record ProvinceOption(String code, String name) {}

    public record Entry(
        UUID id,
        String name,
        String address,
        String category,
        double latitude,
        double longitude,
        String provinceCode,
        String province,
        LocalTime openingTime,
        LocalTime closingTime,
        String weeklyOpeningHours,
        PlaceType placeType,
        List<String> samplePhotoUrls,
        String description,
        List<String> tags,
        int estimatedDurationMinutes,
        String sourceUrl,
        LocalDate reviewedOn,
        String verificationScope,
        String coordinatesQuality,
        String hoursQuality,
        boolean approved,
        Instant archivedAt,
        int version
    ) {}

    public record Page(List<Entry> content, int page, int size, long totalElements) {}

    public record PlaceRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 2000) String address,
        @Size(max = 100) String category,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank @Pattern(regexp = "\\d{2}") String provinceCode,
        LocalTime openingTime,
        LocalTime closingTime,
        @Size(max = 5000) String weeklyOpeningHours,
        @NotNull PlaceType placeType,
        @NotNull @Size(max = 8) List<@NotBlank @Size(max = 2048) String> samplePhotoUrls,
        @NotBlank @Size(max = 5000) String description,
        @NotEmpty @Size(max = 20) List<@NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,29}") String> tags,
        @NotNull @Positive Integer estimatedDurationMinutes,
        @NotBlank @Size(max = 1000) String sourceUrl,
        @NotNull LocalDate reviewedOn,
        @NotBlank @Size(max = 500) String verificationScope,
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,29}") String coordinatesQuality,
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{1,29}") String hoursQuality
    ) {}

    public record Review(@NotNull Boolean approved, @NotNull @Positive Integer version) {}

    @Transactional(readOnly = true)
    public List<ProvinceOption> provinces() {
        return jdbc.query("SELECT code,name FROM provinces WHERE code ~ '^[0-9]{2}$' ORDER BY name,code", (rs, row) ->
            new ProvinceOption(rs.getString("code"), rs.getString("name"))
        );
    }

    @Transactional(readOnly = true)
    public Page list(int page, int size, String provinceCode, String status, String tag, String query) {
        if (
            page < 0 ||
            size < 1 ||
            size > 100 ||
            (query != null && query.length() > 200) ||
            (provinceCode != null && !provinceCode.isBlank() && !provinceCode.matches("\\d{2}"))
        ) throw new AppException(ErrorCode.VALIDATION_ERROR);
        String normalizedStatus = status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ALL", "PENDING", "APPROVED", "ARCHIVED").contains(normalizedStatus)) throw new AppException(
            ErrorCode.VALIDATION_ERROR
        );

        StringBuilder where = new StringBuilder(FROM);
        List<Object> args = new ArrayList<>();
        if (provinceCode != null && !provinceCode.isBlank()) {
            where.append(" AND province.code=?");
            args.add(provinceCode.trim());
        }
        switch (normalizedStatus) {
            case "PENDING" -> where.append(" AND r.archived_at IS NULL AND NOT r.approved");
            case "APPROVED" -> where.append(" AND r.archived_at IS NULL AND r.approved");
            case "ARCHIVED" -> where.append(" AND r.archived_at IS NOT NULL");
            default -> {
            }
        }
        if (tag != null && !tag.isBlank()) {
            String normalizedTag = tag.trim().toUpperCase(Locale.ROOT);
            if (!TAG.matcher(normalizedTag).matches()) throw new AppException(ErrorCode.VALIDATION_ERROR);
            where.append(" AND ? = ANY(r.tags)");
            args.add(normalizedTag);
        }
        if (query != null && !query.isBlank()) {
            String search = "%" + escapeLike(query.trim()) + "%";
            where.append(
                " AND (p.name ILIKE ? ESCAPE E'\\\\' OR p.address ILIKE ? ESCAPE E'\\\\' " +
                    "OR coalesce(r.description,'') ILIKE ? ESCAPE E'\\\\')"
            );
            args.add(search);
            args.add(search);
            args.add(search);
        }

        String select =
            "SELECT p.id,p.name,p.address,p.category,p.latitude,p.longitude,province.code province_code," +
            "province.name province_name,p.opening_time,p.closing_time,p.weekly_opening_hours,p.place_type,p.sample_photo_urls," +
            "r.description,r.tags,r.estimated_duration_minutes,r.source_url,r.reviewed_on,r.verification_scope," +
            "r.coordinates_quality,r.hours_quality,r.approved,r.archived_at,r.content_version" +
            where;
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<Entry> content = jdbc.query(
            select + " ORDER BY p.name,p.id LIMIT ? OFFSET ?",
            this::mapEntry,
            pageArgs.toArray()
        );
        Long total = jdbc.queryForObject("SELECT count(*)" + where, Long.class, args.toArray());
        return new Page(content, page, size, total == null ? 0 : total);
    }

    @Transactional
    public Entry create(PlaceRequest request) {
        validate(request);
        UUID provinceId = provinceId(request.provinceCode());
        ensureUniqueName(request.name(), provinceId, null);
        UUID id = UUID.randomUUID();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                """
                INSERT INTO places(id,source,owner_id,name,address,category,latitude,longitude,province_id,
                    opening_time,closing_time,weekly_opening_hours,place_type,sample_photo_urls)
                VALUES (?,'INTERNAL',NULL,?,?,?,?,?,?,?,?,?,?,?)
                """
            );
            statement.setObject(1, id);
            statement.setString(2, request.name().trim());
            statement.setString(3, request.address().trim());
            statement.setString(4, blankToNull(request.category()));
            statement.setDouble(5, request.latitude());
            statement.setDouble(6, request.longitude());
            statement.setObject(7, provinceId);
            statement.setObject(8, request.openingTime());
            statement.setObject(9, request.closingTime());
            statement.setString(10, blankToNull(request.weeklyOpeningHours()));
            statement.setString(11, request.placeType().name());
            statement.setArray(12, connection.createArrayOf("text", request.samplePhotoUrls().toArray()));
            return statement;
        });
        insertProfile(id, request);
        Entry created = find(id);
        audit(id, "CREATE", null, created);
        return created;
    }

    @Transactional
    public Entry update(UUID id, PlaceRequest request, int version) {
        validate(request);
        Entry before = lock(id);
        checkVersion(before, version);
        if (before.archivedAt() != null) throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        UUID provinceId = provinceId(request.provinceCode());
        ensureUniqueName(request.name(), provinceId, id);
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                """
                UPDATE places SET name=?,address=?,category=?,latitude=?,longitude=?,province_id=?,opening_time=?,
                    closing_time=?,weekly_opening_hours=?,place_type=?,sample_photo_urls=? WHERE id=? AND source='INTERNAL'
                """
            );
            statement.setString(1, request.name().trim());
            statement.setString(2, request.address().trim());
            statement.setString(3, blankToNull(request.category()));
            statement.setDouble(4, request.latitude());
            statement.setDouble(5, request.longitude());
            statement.setObject(6, provinceId);
            statement.setObject(7, request.openingTime());
            statement.setObject(8, request.closingTime());
            statement.setString(9, blankToNull(request.weeklyOpeningHours()));
            statement.setString(10, request.placeType().name());
            statement.setArray(11, connection.createArrayOf("text", request.samplePhotoUrls().toArray()));
            statement.setObject(12, id);
            return statement;
        });
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                """
                UPDATE place_retrieval_profiles SET description=?,tags=?,estimated_duration_minutes=?,source_url=?,
                    reviewed_on=?,verification_scope=?,coordinates_quality=?,hours_quality=?,approved=false,
                    content_version=content_version+1 WHERE place_id=?
                """
            );
            statement.setString(1, request.description().trim());
            statement.setArray(2, connection.createArrayOf("text", request.tags().toArray()));
            statement.setInt(3, request.estimatedDurationMinutes());
            statement.setString(4, request.sourceUrl().trim());
            statement.setObject(5, request.reviewedOn());
            statement.setString(6, request.verificationScope().trim());
            statement.setString(7, request.coordinatesQuality());
            statement.setString(8, request.hoursQuality());
            statement.setObject(9, id);
            return statement;
        });
        Entry updated = find(id);
        if (before.approved()) recordReviewEvent(id, true, false);
        audit(id, "UPDATE", before, updated);
        return updated;
    }

    @Transactional
    public Entry archive(UUID id, int version) {
        Entry before = lock(id);
        checkVersion(before, version);
        if (before.archivedAt() != null) return before;
        jdbc.update(
            "UPDATE place_retrieval_profiles SET archived_at=now(),approved=false,content_version=content_version+1 WHERE place_id=?",
            id
        );
        Entry archived = find(id);
        if (before.approved()) recordReviewEvent(id, true, false);
        audit(id, "ARCHIVE", before, archived);
        return archived;
    }

    @Transactional
    public Entry restore(UUID id, int version) {
        Entry before = lock(id);
        checkVersion(before, version);
        if (before.archivedAt() == null) return before;
        jdbc.update(
            "UPDATE place_retrieval_profiles SET archived_at=NULL,approved=false,content_version=content_version+1 WHERE place_id=?",
            id
        );
        Entry restored = find(id);
        audit(id, "RESTORE", before, restored);
        return restored;
    }

    @Transactional
    public void review(UUID id, Review request) {
        Entry before = lock(id);
        checkVersion(before, request.version());
        if (before.archivedAt() != null) throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        if (before.approved() == request.approved()) return;
        jdbc.update(
            "UPDATE place_retrieval_profiles SET approved=?,content_version=content_version+1 WHERE place_id=?",
            request.approved(),
            id
        );
        Entry after = find(id);
        recordReviewEvent(id, before.approved(), request.approved());
        audit(id, "REVIEW", before, after);
    }

    private void validate(PlaceRequest request) {
        if ((request.openingTime() == null) != (request.closingTime() == null)) throw new AppException(
            ErrorCode.VALIDATION_ERROR
        );
        if (
            request.openingTime() != null && !request.openingTime().isBefore(request.closingTime())
        ) throw new AppException(ErrorCode.VALIDATION_ERROR);
        if (request.tags().stream().distinct().count() != request.tags().size()) throw new AppException(
            ErrorCode.VALIDATION_ERROR
        );
        if (
            request.samplePhotoUrls().size() > 8 ||
            request.samplePhotoUrls().stream().distinct().count() != request.samplePhotoUrls().size() ||
            request
                .samplePhotoUrls()
                .stream()
                .anyMatch(url -> !isHttpsUrl(url))
        ) throw new AppException(ErrorCode.VALIDATION_ERROR);
        try {
            URI source = URI.create(request.sourceUrl().trim());
            if (
                source.getScheme() == null ||
                !Set.of("http", "https").contains(source.getScheme().toLowerCase(Locale.ROOT)) ||
                source.getHost() == null
            ) throw new AppException(ErrorCode.VALIDATION_ERROR);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private UUID provinceId(String code) {
        List<UUID> ids = jdbc.query(
            "SELECT id FROM provinces WHERE code=? AND code ~ '^[0-9]{2}$'",
            (rs, row) -> rs.getObject("id", UUID.class),
            code
        );
        if (ids.isEmpty()) throw new AppException(ErrorCode.PROVINCE_NOT_FOUND);
        return ids.getFirst();
    }

    private void ensureUniqueName(String name, UUID provinceId, UUID exceptId) {
        Long matches = jdbc.queryForObject(
            """
            SELECT count(*) FROM places p
            LEFT JOIN place_retrieval_profiles r ON r.place_id=p.id
            WHERE p.source='INTERNAL' AND p.province_id=? AND lower(btrim(p.name))=lower(btrim(?))
                AND (r.archived_at IS NULL OR r.place_id IS NULL)
                AND (CAST(? AS uuid) IS NULL OR p.id<>?)
            """,
            Long.class,
            provinceId,
            name,
            exceptId,
            exceptId
        );
        if (matches != null && matches > 0) throw new AppException(ErrorCode.PLACE_CATALOG_DUPLICATE);
    }

    private void recordReviewEvent(UUID id, boolean wasApproved, boolean isApproved) {
        jdbc.update(
            "INSERT INTO admin_catalog_events(actor_id,place_id,was_approved,is_approved) VALUES (?,?,?,?)",
            SecurityUtils.getCurrentUserId(),
            id,
            wasApproved,
            isApproved
        );
    }

    private void insertProfile(UUID id, PlaceRequest request) {
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                """
                INSERT INTO place_retrieval_profiles(place_id,description,tags,estimated_duration_minutes,
                    source_url,reviewed_on,verification_scope,coordinates_quality,hours_quality,approved)
                VALUES (?,?,?,?,?,?,?,?,?,false)
                """
            );
            statement.setObject(1, id);
            statement.setString(2, request.description().trim());
            statement.setArray(3, connection.createArrayOf("text", request.tags().toArray()));
            statement.setInt(4, request.estimatedDurationMinutes());
            statement.setString(5, request.sourceUrl().trim());
            statement.setObject(6, request.reviewedOn());
            statement.setString(7, request.verificationScope().trim());
            statement.setString(8, request.coordinatesQuality());
            statement.setString(9, request.hoursQuality());
            return statement;
        });
    }

    private Entry lock(UUID id) {
        List<Entry> entries = jdbc.query(
            "SELECT p.id,p.name,p.address,p.category,p.latitude,p.longitude," +
                "province.code province_code,province.name province_name,p.opening_time,p.closing_time," +
                "p.weekly_opening_hours,p.place_type,p.sample_photo_urls,r.description,r.tags,r.estimated_duration_minutes," +
                "r.source_url,r.reviewed_on,r.verification_scope,r.coordinates_quality,r.hours_quality," +
                "r.approved,r.archived_at,r.content_version" +
                FROM +
                " AND p.id=? FOR UPDATE OF p,r",
            this::mapEntry,
            id
        );
        if (entries.isEmpty()) throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        return entries.getFirst();
    }

    private Entry find(UUID id) {
        List<Entry> entries = jdbc.query(
            "SELECT p.id,p.name,p.address,p.category,p.latitude,p.longitude," +
                "province.code province_code,province.name province_name,p.opening_time,p.closing_time," +
                "p.weekly_opening_hours,p.place_type,p.sample_photo_urls,r.description,r.tags,r.estimated_duration_minutes," +
                "r.source_url,r.reviewed_on,r.verification_scope,r.coordinates_quality,r.hours_quality," +
                "r.approved,r.archived_at,r.content_version" +
                FROM +
                " AND p.id=?",
            this::mapEntry,
            id
        );
        if (entries.isEmpty()) throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        return entries.getFirst();
    }

    private void checkVersion(Entry entry, int version) {
        if (entry.version() != version) throw new AppException(ErrorCode.ADMIN_CATALOG_CONFLICT);
    }

    private Entry mapEntry(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        java.sql.Array tagArray = rs.getArray("tags");
        List<String> tags = tagArray == null ? List.of() : Arrays.asList((String[]) tagArray.getArray());
        String type = rs.getString("place_type");
        java.sql.Array photoArray = rs.getArray("sample_photo_urls");
        List<String> samplePhotoUrls = photoArray == null ? List.of() : Arrays.asList((String[]) photoArray.getArray());
        return new Entry(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getString("address"),
            rs.getString("category"),
            rs.getDouble("latitude"),
            rs.getDouble("longitude"),
            rs.getString("province_code"),
            rs.getString("province_name"),
            rs.getObject("opening_time", LocalTime.class),
            rs.getObject("closing_time", LocalTime.class),
            rs.getString("weekly_opening_hours"),
            type == null ? PlaceType.UNSPECIFIED : PlaceType.valueOf(type),
            samplePhotoUrls,
            rs.getString("description"),
            tags,
            rs.getInt("estimated_duration_minutes"),
            rs.getString("source_url"),
            rs.getObject("reviewed_on", LocalDate.class),
            rs.getString("verification_scope"),
            rs.getString("coordinates_quality"),
            rs.getString("hours_quality"),
            rs.getBoolean("approved"),
            Optional.ofNullable(rs.getTimestamp("archived_at")).map(java.sql.Timestamp::toInstant).orElse(null),
            rs.getInt("content_version")
        );
    }

    private void audit(UUID id, String action, Entry before, Entry after) {
        jdbc.update(
            "INSERT INTO admin_catalog_audit_events(actor_id,place_id,action,before_data,after_data) " +
                "VALUES (?,?,?,?::jsonb,?::jsonb)",
            SecurityUtils.getCurrentUserId(),
            id,
            action,
            json(before),
            json(after)
        );
    }

    private String json(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize catalog audit data", ex);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
