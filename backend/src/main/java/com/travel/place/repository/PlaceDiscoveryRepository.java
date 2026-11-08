package com.travel.place.repository;

import com.travel.place.dto.PlaceDiscovery.Category;
import com.travel.place.dto.PlaceDiscovery.Location;
import com.travel.place.dto.PlaceDiscovery.Reference;
import com.travel.place.dto.PlaceDiscovery.Summary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlaceDiscoveryRepository {

    private final JdbcTemplate jdbc;
    private static final String ACCENTS = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";

    public record Filter(
            String sql,
            List<Object> args
    ) {}

    public Filter filter(UUID province, UUID trip, String query) {
        String sql = """
             WHERE p.source='INTERNAL' AND p.province_id=?
               AND EXISTS (SELECT 1 FROM place_retrieval_profiles r WHERE r.place_id=p.id AND r.approved)
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
        return Objects.requireNonNull(jdbc.queryForObject(
                "SELECT count(*) FROM places p" + filter.sql(),
                Long.class,
                filter.args().toArray()
        ));
    }

    public List<Summary> page(Filter filter, int size, long offset) {
        List<Object> args = new ArrayList<>(filter.args());
        args.add(size);
        args.add(offset);
        return jdbc.query("SELECT p.* FROM places p" + filter.sql() + " ORDER BY p.name, p.id LIMIT ? OFFSET ?",
                (r, index) -> new Summary(
                        r.getObject("id", UUID.class),
                        new Reference("INTERNAL", r.getObject("id", UUID.class)),
                        "INTERNAL",
                        r.getString("name"),
                        r.getString("address"),
                        new Location(r.getDouble("latitude"), r.getDouble("longitude")),
                        r.getObject("province_id", UUID.class),
                        r.getString("category") == null ? null : new Category(r.getString("category"), r.getString("category")),
                        null
                ), args.toArray());
    }

    private static String fold(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
    }
}
