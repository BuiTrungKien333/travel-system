package com.travel.place.repository;

import com.travel.place.dto.DestinationPage.Option;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read only bounded Province projections; no provider lookup, extension, or entity graph. */
@Repository
@RequiredArgsConstructor
public class DestinationCatalogRepository {

    private final JdbcTemplate jdbc;
    private static final String ACCENTS = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
    private static final String FOLDED = fold(ACCENTS);
    private static final String FILTER = """
    WHERE translate(lower(name), ?, ?) LIKE ? ESCAPE E'\\\\'
       OR lower(code) LIKE ? ESCAPE E'\\\\'
    """;

    public record Search(String clause, List<Object> arguments) {}

    public Search search(String query) {
        if (query.isEmpty()) return new Search("", List.of());
        String pattern = "%" + fold(query).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return new Search(FILTER, List.of(ACCENTS, FOLDED, pattern, pattern));
    }

    public long count(Search search) {
        return Objects.requireNonNull(
            jdbc.queryForObject(
                "SELECT count(*) FROM provinces " + search.clause(),
                Long.class,
                search.arguments().toArray()
            )
        );
    }

    public List<Option> page(Search search, int size, long offset) {
        List<Object> arguments = new ArrayList<>(search.arguments());
        arguments.add(size);
        arguments.add(offset);
        return jdbc.query(
            "SELECT id,code,name FROM provinces " + search.clause() + " ORDER BY code,id LIMIT ? OFFSET ?",
            (row, index) -> new Option(row.getObject("id", UUID.class), row.getString("code"), row.getString("name")),
            arguments.toArray()
        );
    }

    private static String fold(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace('đ', 'd');
    }
}
