package com.travel.planning.retrieval.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.ai.resilience.AiCallFailure;
import com.travel.planning.retrieval.config.RagProperties;
import com.travel.planning.retrieval.dto.response.PlaceIndexResponse;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse.Match;
import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.retrieval.provider.GeminiEmbeddingClient;
import com.travel.planning.retrieval.repository.PlaceRetrievalRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Profile({"dev", "rag-demo", "planning-ai"})
public class PlaceRetrievalService {
    private final PlaceRetrievalRepository repository;
    private final GeminiEmbeddingClient embeddings;
    private final RagProperties properties;
    private final AtomicBoolean indexing = new AtomicBoolean();

    public PlaceRetrievalService(PlaceRetrievalRepository repository, GeminiEmbeddingClient embeddings,
                                 RagProperties properties) {
        this.repository = repository;
        this.embeddings = embeddings;
        this.properties = properties;
    }

    public List<PlaceCandidate> catalog() {
        return repository.catalog();
    }

    public PlaceIndexResponse index() {
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
            return new PlaceIndexResponse(updated, skipped, failed, properties.getEmbeddingModel());
        } finally {
            indexing.set(false);
        }
    }

    public PlaceSearchResponse search(String positiveQuery, List<String> excludedTags, int limit) {
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
        Set<UUID> lexicalIds = new HashSet<>(lexical);
        Set<UUID> semanticIds = new HashSet<>(semantic);
        // Hydrate only approved, existing DB rows. Rankings never create places.
        List<Match> matches = repository.candidates(scores.keySet()).stream()
                .filter(p -> p.tags().stream().noneMatch(excluded::contains))
                .map(p -> new Match(p, scores.get(p.placeId()), lexicalIds.contains(p.placeId()), semanticIds.contains(p.placeId())))
                .sorted(Comparator.comparingDouble(Match::rankScore).reversed().thenComparing(m -> m.place().placeId()))
                .limit(limit).toList();
        return new PlaceSearchResponse(mode, properties.getEmbeddingModel(), matches, fallbackReason);
    }

    private void addRanks(Map<UUID, Double> scores, List<UUID> ids) {
        for (int i = 0; i < ids.size(); i++) scores.merge(ids.get(i), 1.0 / (60 + i + 1), Double::sum);
    }

}
