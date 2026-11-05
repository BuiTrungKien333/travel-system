package com.travel.planning.retrieval;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.resilience.AiCallFailure;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile("rag-demo")
public class PlaceRetrievalService {
    private final PlaceRetrievalRepository repository;
    private final GeminiEmbeddingClient embeddings;
    private final RagDemoProperties properties;
    private final AtomicBoolean indexing = new AtomicBoolean();

    public PlaceRetrievalService(PlaceRetrievalRepository repository, GeminiEmbeddingClient embeddings,
                                 RagDemoProperties properties) {
        this.repository = repository;
        this.embeddings = embeddings;
        this.properties = properties;
    }

    public List<PlaceCandidate> catalog() {
        return repository.catalog();
    }

    public IndexResult index() {
        if (!indexing.compareAndSet(false, true)) throw new AppException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        int updated = 0, skipped = 0, failed = 0;
        try {
            for (var place : repository.catalog()) {
                if (repository.indexed(place, properties.getEmbeddingModel())) {
                    skipped++;
                    continue;
                }
                try {
                    float[] vector = embeddings.embed(place.document(), true);
                    if (repository.saveEmbedding(place, properties.getEmbeddingModel(), vector)) updated++;
                    else failed++;
                } catch (AppException ex) {
                    failed++;
                }
            }
            return new IndexResult(updated, skipped, failed, properties.getEmbeddingModel());
        } finally {
            indexing.set(false);
        }
    }

    public SearchResult search(String positiveQuery, List<String> excludedTags, int limit) {
        List<UUID> lexical = repository.lexical(positiveQuery);
        List<UUID> semantic;
        String mode;
        String fallbackReason = null;
        try {
            semantic = repository.semantic(embeddings.embed(positiveQuery, false), properties.getEmbeddingModel());
            mode = semantic.isEmpty() ? "LEXICAL_ONLY_NO_CURRENT_EMBEDDINGS" : "HYBRID";
            if (semantic.isEmpty()) fallbackReason = "NO_CURRENT_EMBEDDINGS";
        } catch (AppException ex) {
            semantic = List.of();
            mode = "LEXICAL_FALLBACK_EMBEDDING_UNAVAILABLE";
            fallbackReason = ex instanceof AiCallFailure failure ? failure.reason() : "EMBEDDING_UNAVAILABLE";
        }
        Map<UUID, Double> scores = new HashMap<>();
        addRanks(scores, lexical);
        addRanks(scores, semantic);
        Set<String> excluded = new HashSet<>(excludedTags);
        List<UUID> semanticIds = semantic;
        // Hydrate only approved, existing DB rows. Rankings never create places.
        List<Match> matches = repository.catalog().stream()
                .filter(p -> scores.containsKey(p.placeId()))
                .filter(p -> p.tags().stream().noneMatch(excluded::contains))
                .map(p -> new Match(p, scores.get(p.placeId()), lexical.contains(p.placeId()), semanticIds.contains(p.placeId())))
                .sorted(Comparator.comparingDouble(Match::rankScore).reversed().thenComparing(m -> m.place().placeId()))
                .limit(limit).toList();
        return new SearchResult(mode, properties.getEmbeddingModel(), matches, fallbackReason);
    }

    private void addRanks(Map<UUID, Double> scores, List<UUID> ids) {
        for (int i = 0; i < ids.size(); i++) scores.merge(ids.get(i), 1.0 / (60 + i + 1), Double::sum);
    }

    public record Match(PlaceCandidate place, double rankScore, boolean lexicalMatch, boolean semanticMatch) {
    }

    public record SearchResult(String retrievalMode, String embeddingModel, List<Match> matches,
                               String fallbackReason) {
        public SearchResult(String retrievalMode, String embeddingModel, List<Match> matches) {
            this(retrievalMode, embeddingModel, matches, null);
        }
    }

    public record IndexResult(int updated, int skipped, int failed, String model) {
    }
}
