package com.travel.planning.retrieval.repository;

import com.travel.planning.retrieval.model.PlaceCandidate;
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
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Repository
@Profile({"dev", "rag-demo"})
public class PlaceRetrievalRepository {

    private final JdbcTemplate jdbc;

    private static final String JOINS = """
             FROM places p JOIN place_retrieval_profiles r ON r.place_id = p.id
             JOIN provinces province ON province.id = p.province_id
            """;
    private static final String DOCUMENT = "(p.name || E'\\n' || r.description || E'\\n' || array_to_string(r.tags, ', '))";
    private static final String HASH = "encode(sha256(convert_to(" + DOCUMENT + ", 'UTF8')), 'hex')";

    public PlaceRetrievalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PlaceCandidate> catalog() {
        return jdbc.query("SELECT p.*, r.* " + JOINS
                + " WHERE r.approved AND province.code = '48' ORDER BY p.name", this::map);
    }

    public List<UUID> semantic(float[] vector, String model) {
        return jdbc.query("SELECT p.id " + JOINS + """
                         JOIN place_embeddings e ON e.place_id = p.id
                         WHERE r.approved AND province.code = '48' AND e.model = ?
                         AND e.content_hash =
                        """ + HASH + " ORDER BY e.embedding <=> CAST(? AS vector), p.id LIMIT 40",
                (rs, row) -> rs.getObject("id", UUID.class), model, GeminiEmbeddingClient.vectorLiteral(vector));
    }

    public List<UUID> lexical(String query) {
        String textVector = "to_tsvector('simple', unaccent(" + DOCUMENT + "))";
        String textQuery = "websearch_to_tsquery('simple', unaccent(?))";

        return jdbc.query("SELECT p.id " + JOINS + " WHERE r.approved AND province.code = '48' AND "
                        + textVector + " @@ " + textQuery + " ORDER BY ts_rank(" + textVector + ", "
                        + textQuery + ") DESC, p.id LIMIT 40",
                (rs, row) -> rs.getObject("id", UUID.class), query, query);
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
                """ + JOINS + " WHERE p.id=? AND r.approved AND " + HASH + " = ? " + """
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
