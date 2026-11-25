package com.travel.place.repository;

import com.travel.place.dto.PlaceDiscovery.Category;
import com.travel.place.dto.PlaceDiscovery.Location;
import com.travel.place.dto.PlaceDiscovery.Reference;
import com.travel.place.dto.PlaceDiscovery.Summary;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceDiscoveryRepository {

    private final JdbcTemplate jdbc;
    private static final String ACCENTS = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";

    public record Filter(String sql, List<Object> args) {}

    public Filter filter(UUID province, UUID trip, String query) {
        String sql = """
         WHERE p.source='INTERNAL' AND p.province_id=?
           AND EXISTS (SELECT 1 FROM place_retrieval_profiles r WHERE r.place_id=p.id AND r.approved AND r.archived_at IS NULL)
        """;
        List<Object> args = new ArrayList<>();
        args.add(province);

        if (trip != null) {
            sql += " AND NOT EXISTS (SELECT 1 FROM trip_places tp WHERE tp.trip_id=? AND tp.place_id=p.id)";
            args.add(trip);
        }
        if (!query.isEmpty()) {
            sql += " AND translate(lower(p.name || ' ' || coalesce(p.address,'')),?,?) LIKE ? ESCAPE E'\\\\'";
            args.add(ACCENTS);
            args.add(fold(ACCENTS));
            args.add("%" + fold(query).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        return new Filter(sql, args);
    }

    public long count(Filter filter) {
        return Objects.requireNonNull(
            jdbc.queryForObject("SELECT count(*) FROM places p" + filter.sql(), Long.class, filter.args().toArray())
        );
    }

    public boolean isPublished(UUID id) {
        return (
            jdbc.queryForObject(
                "SELECT count(*) FROM places p JOIN place_retrieval_profiles r ON r.place_id=p.id WHERE p.id=? AND p.source='INTERNAL' AND r.approved AND r.archived_at IS NULL",
                Long.class,
                id
            ) == 1
        );
    }

    public List<Summary> page(Filter filter, int size, long offset) {
        List<Object> args = new ArrayList<>(filter.args());
        args.add(size);
        args.add(offset);
        return jdbc.query(
            "SELECT p.* FROM places p" + filter.sql() + " ORDER BY p.name, p.id LIMIT ? OFFSET ?",
            (r, index) ->
                new Summary(
                    r.getObject("id", UUID.class),
                    new Reference("INTERNAL", r.getObject("id", UUID.class)),
                    "INTERNAL",
                    r.getString("name"),
                    r.getString("address"),
                    new Location(r.getDouble("latitude"), r.getDouble("longitude")),
                    r.getObject("province_id", UUID.class),
                    r.getString("category") == null
                        ? null
                        : new Category(r.getString("category"), r.getString("category")),
                    firstPhoto(r.getArray("sample_photo_urls"))
                ),
            args.toArray()
        );
    }

    public List<String> samplePhotoUrls(UUID placeId) {
        return jdbc.query(
            "SELECT sample_photo_urls FROM places WHERE id=?",
            rs -> {
                if (!rs.next()) return List.of();
                java.sql.Array urls = rs.getArray(1);
                return urls == null ? List.of() : Arrays.asList((String[]) urls.getArray());
            },
            placeId
        );
    }

    private static com.travel.place.dto.PlaceDiscovery.Photo firstPhoto(
        java.sql.Array values
    ) throws java.sql.SQLException {
        if (values == null) return null;
        String[] urls = (String[]) values.getArray();
        return urls.length == 0
            ? null
            : new com.travel.place.dto.PlaceDiscovery.Photo(urls[0], "ADMIN_SAMPLE", List.of());
    }

    private static String fold(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace('đ', 'd');
    }
}
