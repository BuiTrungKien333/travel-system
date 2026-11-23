package com.travel.planning.retrieval;

import com.travel.planning.retrieval.config.RagProperties;
import com.travel.planning.retrieval.provider.GeminiEmbeddingClient;
import com.travel.planning.retrieval.repository.PlaceRetrievalRepository;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RagCatalogIntegrationTest {
    static PostgreSQLContainer<?> postgres;
    static JdbcTemplate jdbc;
    static PlaceRetrievalRepository repository;
    static Flyway flyway;

    @BeforeAll
    static void database() {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:0.8.2-pg17")
                .asCompatibleSubstituteFor("postgres"));
        postgres.start();
        var ds = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        flyway = Flyway.configure().dataSource(ds).locations("classpath:db/migration", "classpath:db/rag").load();
        flyway.migrate();
        jdbc = new JdbcTemplate(ds);
        repository = new PlaceRetrievalRepository(jdbc);
    }

    @AfterAll
    static void stop() { if (postgres != null) postgres.stop(); }

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM place_embeddings");
        jdbc.update("UPDATE place_retrieval_profiles SET approved=true");
        jdbc.update("UPDATE places SET source='INTERNAL',owner_id=NULL");
    }

    @Test
    void migrationAndSeed_repeatWithoutDuplicatesAndExposeProvenance() {
        flyway.migrate();
        assertThat(repository.catalog()).hasSize(8).allSatisfy(p -> {
            assertThat(p.placeId()).isNotNull();
            assertThat(p.sourceUrl()).startsWith("https://");
            assertThat(p.hoursQuality()).isEqualTo("UNKNOWN");
            assertThat(p.coordinatesQuality()).isEqualTo("APPROXIMATE_DEMO");
            assertThat(p.openingTime()).isNull();
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM places", Integer.class)).isEqualTo(8);
    }

    @Test
    void candidates_loadsOnlyRequestedApprovedPublicPlaces() {
        var catalog = repository.catalog();
        var first = catalog.get(0);
        var second = catalog.get(1);
        var third = catalog.get(2);
        var ids = List.of(first.placeId(), second.placeId(), third.placeId(), UUID.randomUUID());
        jdbc.update("UPDATE places SET source='USER',owner_id='b0000000-0000-0000-0000-000000000002' WHERE id=?", second.placeId());
        jdbc.update("UPDATE place_retrieval_profiles SET approved=false WHERE place_id=?", third.placeId());

        assertThat(repository.candidates(ids)).containsExactly(first);
        assertThat(repository.candidates(List.of())).isEmpty();
    }

    @Test
    void lexical_matchesVietnameseWithAndWithoutAccents() {
        assertThat(repository.lexical("bảo tàng")).hasSize(3);
        assertThat(repository.lexical("bao tang")).hasSize(3);
        assertThat(repository.lexical("ZZZ_NO_SUCH_PLACE")).isEmpty();
    }

    @Test
    void privatePlacesCannotEnterPublicRetrievalOrEmbeddingEvenWithAnApprovedProfile() {
        var place = repository.catalog().getFirst();
        var vector = vector(1,0);
        assertThat(repository.saveEmbedding(place,"fixture",vector)).isTrue();
        jdbc.update("UPDATE places SET source='USER',owner_id='b0000000-0000-0000-0000-000000000002' WHERE id=?",place.placeId());
        assertThat(repository.catalog()).extracting(candidate -> candidate.placeId()).doesNotContain(place.placeId());
        assertThat(repository.lexical(place.name())).doesNotContain(place.placeId());
        assertThat(repository.semantic(vector,"fixture")).isEmpty();
        assertThat(repository.saveEmbedding(place,"fixture",vector)).isFalse();
    }

    @Test
    void exactVectorSearch_ordersFixturesAndOmitsUnapprovedAndStaleContent() {
        var places = repository.catalog();
        var first = places.get(0); var second = places.get(1);
        var q = vector(1, 0);
        assertThat(repository.saveEmbedding(first, "fixture", q)).isTrue();
        assertThat(repository.saveEmbedding(second, "fixture", vector(0, 1))).isTrue();
        assertThat(repository.semantic(q, "fixture")).containsExactly(first.placeId(), second.placeId());
        jdbc.update("UPDATE place_embeddings SET content_hash='stale' WHERE place_id=?", second.placeId());
        assertThat(repository.semantic(q, "fixture")).containsExactly(first.placeId());
        jdbc.update("UPDATE place_retrieval_profiles SET approved=false WHERE place_id=?", first.placeId());
        assertThat(repository.semantic(q, "fixture")).isEmpty();
        assertThat(repository.catalog()).hasSize(7);
    }

    @Test
    void indexing_reusesCurrentContentAndRejectsUnapprovedPlace() {
        var place = repository.catalog().getFirst();
        assertThat(repository.indexed(place, "fixture")).isFalse();
        assertThat(repository.saveEmbedding(place, "fixture", vector(1, 0))).isTrue();
        assertThat(repository.indexed(place, "fixture")).isTrue();
        jdbc.update("UPDATE place_retrieval_profiles SET approved=false WHERE place_id=?", place.placeId());
        assertThat(repository.saveEmbedding(place, "fixture", vector(0, 1))).isFalse();
    }

    @Test
    void foreignKey_rejectsInventedPlaceId() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO place_embeddings(place_id, model, content_hash, embedding)
                VALUES (?, 'fixture', 'test', CAST(? AS vector))
                """, UUID.randomUUID(), GeminiEmbeddingClient.vectorLiteral(vector(1, 0))))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void search_providerUnavailableReportsLexicalFallbackAndHonorsExclusion() {
        var service = new PlaceRetrievalService(repository, new GeminiEmbeddingClient(new RagProperties()), new RagProperties());
        var result = service.search("bao tang", List.of(), 8);
        assertThat(result.retrievalMode()).isEqualTo("LEXICAL_FALLBACK_EMBEDDING_UNAVAILABLE");
        assertThat(result.matches()).hasSize(3).allSatisfy(m -> assertThat(m.lexicalMatch()).isTrue());
        assertThat(service.search("bao tang", List.of("MUSEUM"), 8).matches()).isEmpty();
    }

    @Test
    void search_andIndexUseRealRepositoryWithOnlyEmbeddingBoundaryMocked() {
        var properties = new RagProperties(); properties.setEmbeddingModel("fixture");
        var embeddings = mock(GeminiEmbeddingClient.class);
        when(embeddings.embed(anyString(), anyBoolean())).thenReturn(vector(1, 0));
        var service = new PlaceRetrievalService(repository, embeddings, properties);
        assertThat(service.index().updated()).isEqualTo(8);
        assertThat(service.index().skipped()).isEqualTo(8);
        var result = service.search("nghệ thuật", List.of("BEACH", "MUSEUM"), 8);
        assertThat(result.retrievalMode()).isEqualTo("HYBRID");
        assertThat(result.matches()).isNotEmpty().allSatisfy(m -> {
            assertThat(m.place().tags()).doesNotContain("BEACH", "MUSEUM");
            assertThat(m.semanticMatch()).isTrue();
        });
        verify(embeddings, times(8)).embed(anyString(), eq(true));
    }

    static float[] vector(float x, float y) { var v = new float[768]; v[0] = x; v[1] = y; return v; }
}
