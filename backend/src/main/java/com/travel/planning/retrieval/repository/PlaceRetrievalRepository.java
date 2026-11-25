package com.travel.planning.retrieval.repository;

import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.retrieval.model.ProvinceOption;
import com.travel.planning.retrieval.provider.GeminiEmbeddingClient;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Repository
@Profile({"dev", "rag-demo", "planning-ai"})
public class PlaceRetrievalRepository {

    private final JdbcTemplate jdbc;

    private static final String JOINS = """
             FROM places p JOIN place_retrieval_profiles r ON r.place_id = p.id
                AND r.archived_at IS NULL
             JOIN provinces province ON province.id = p.province_id
            """;
    private static final String DOCUMENT = "(p.name || E'\\n' || r.description || E'\\n' || array_to_string(r.tags, ', '))";
    private static final String HASH = "encode(sha256(convert_to(" + DOCUMENT + ", 'UTF8')), 'hex')";
    private static final String CATALOG = "SELECT p.*, r.* " + JOINS
            + " WHERE r.approved AND p.source='INTERNAL'";

    public PlaceRetrievalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PlaceCandidate> catalog() {
        return jdbc.query(CATALOG + " ORDER BY p.name", this::map);
    }

    public List<ProvinceOption> provinces() {
        return jdbc.query("SELECT code,name FROM provinces WHERE code ~ '^[0-9]{2}$' ORDER BY name,code",
                (rs, row) -> new ProvinceOption(rs.getString("code"), rs.getString("name")));
    }

    public String provinceCode(UUID provinceId) {
        if (provinceId == null) return null;
        List<String> codes = jdbc.query("SELECT code FROM provinces WHERE id = ? AND code ~ '^[0-9]{2}$'",
                (rs, row) -> rs.getString("code"), provinceId);
        return codes.isEmpty() ? null : codes.getFirst();
    }

    public List<PlaceCandidate> catalog(String provinceCode) {
        if (provinceCode == null || provinceCode.isBlank()) return List.of();
        return jdbc.query(CATALOG + " AND province.code=? ORDER BY p.name", this::map, provinceCode);
    }

    public List<PlaceCandidate> candidates(Collection<UUID> ids, String provinceCode) {
        if (ids.isEmpty()) return List.of();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String provinceFilter = provinceCode == null || provinceCode.isBlank() ? "" : " AND province.code=?";
        Object[] args = provinceCode == null || provinceCode.isBlank()
                ? ids.toArray()
                : java.util.stream.Stream.concat(java.util.stream.Stream.of(provinceCode), ids.stream()).toArray();
        return jdbc.query(CATALOG + provinceFilter + " AND p.id IN (" + placeholders + ") ORDER BY p.name", this::map, args);
    }

    public List<PlaceCandidate> candidates(Collection<UUID> ids) {
        return candidates(ids, null);
    }

    public List<UUID> semantic(float[] vector, String model, String provinceCode) {
        String provinceFilter = provinceCode == null || provinceCode.isBlank() ? "" : " AND province.code=?";
        String sql = "SELECT p.id " + JOINS + " JOIN place_embeddings e ON e.place_id=p.id "
                + "WHERE r.approved AND p.source='INTERNAL' AND e.model=? AND e.content_hash=" + HASH
                + provinceFilter + " ORDER BY e.embedding <=> CAST(? AS vector),p.id LIMIT 40";
        if (provinceCode == null || provinceCode.isBlank())
            return jdbc.query(sql, (rs, row) -> rs.getObject("id", UUID.class), model, GeminiEmbeddingClient.vectorLiteral(vector));
        return jdbc.query(sql, (rs, row) -> rs.getObject("id", UUID.class), model, provinceCode, GeminiEmbeddingClient.vectorLiteral(vector));
    }

    public List<UUID> semantic(float[] vector, String model) {
        return semantic(vector, model, null);
    }

    public List<UUID> lexical(String query, String provinceCode) {
        String textVector = "to_tsvector('simple', unaccent(" + DOCUMENT + "))";
        String textQuery = "websearch_to_tsquery('simple', unaccent(?))";
        String provinceFilter = provinceCode == null || provinceCode.isBlank() ? "" : " AND province.code=?";
        String sql = "SELECT p.id " + JOINS + " WHERE r.approved AND p.source='INTERNAL' AND "
                + textVector + " @@ " + textQuery + provinceFilter + " ORDER BY ts_rank(" + textVector + ", "
                + textQuery + ") DESC,p.id LIMIT 40";
        if (provinceCode == null || provinceCode.isBlank())
            return jdbc.query(sql, (rs, row) -> rs.getObject("id", UUID.class), query, query);
        return jdbc.query(sql, (rs, row) -> rs.getObject("id", UUID.class), query, provinceCode, query);
    }

    public List<UUID> lexical(String query) {
        return lexical(query, null);
    }

    public boolean indexed(PlaceCandidate place, String model) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM place_embeddings WHERE place_id=? AND model=? AND content_hash=?",
                Integer.class, place.placeId(), model, hash(place));
        return count != null && count > 0;
    }

    public boolean saveEmbedding(PlaceCandidate place, String model, float[] vector) {
        return jdbc.update("""
                INSERT INTO place_embeddings(place_id, model, content_hash, embedding)
                SELECT p.id, ?, ?, CAST(? AS vector)
                """ + JOINS + " WHERE p.id=? AND r.approved AND p.source='INTERNAL' AND " + HASH + " = ? " + """
                ON CONFLICT (place_id, model) DO UPDATE SET content_hash=EXCLUDED.content_hash,
                    embedding=EXCLUDED.embedding, updated_at=now()
                """, model, hash(place), GeminiEmbeddingClient.vectorLiteral(vector), place.placeId(), hash(place)) > 0;
    }

    public static String hash(PlaceCandidate place) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(place.document().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private PlaceCandidate map(ResultSet rs, int row) throws SQLException {
        return new PlaceCandidate(rs.getObject("place_id", UUID.class), rs.getString("name"),
                rs.getString("description"), Arrays.asList((String[]) rs.getArray("tags").getArray()),
                rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("place_type"),
                rs.getObject("opening_time", LocalTime.class), rs.getObject("closing_time", LocalTime.class),
                rs.getInt("estimated_duration_minutes"), rs.getString("source_url"),
                rs.getObject("reviewed_on", LocalDate.class), rs.getString("verification_scope"),
                rs.getString("coordinates_quality"), rs.getString("hours_quality"), rs.getInt("content_version"));
    }
}
