package com.travel.planning.ai.resilience;

import com.travel.planning.retrieval.RagDemoProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.*;
import java.util.function.LongSupplier;

@Component
@Profile("rag-demo")
public class AiRequestContext {
    public enum Stage { INTENT, RETRIEVAL, ALLOCATION, OPTIMIZATION, THEMES, WEATHER }
    public record StageResult(Stage stage, String source, String method, String model, int attempts,
                              int retries, String reason, Integer lastProviderStatus, String retryDecision) {}
    public record Report(String overallMode, long deadlineMillis, long elapsedMillis, int retriesUsed,
                         List<StageResult> stages) {}
    private final ThreadLocal<Budget> local = new ThreadLocal<>();
    private final RagDemoProperties properties;
    private final LongSupplier nanoTime;

    @Autowired
    public AiRequestContext(RagDemoProperties properties) { this(properties, System::nanoTime); }
    public AiRequestContext(RagDemoProperties properties, LongSupplier nanoTime) {
        this.properties = properties; this.nanoTime = nanoTime;
    }
    public Scope ensure() {
        if (local.get() != null) return new Scope(false);
        local.set(new Budget(nanoTime.getAsLong(), Math.max(1, properties.getRequestDeadlineMillis())));
        return new Scope(true);
    }
    public long remainingMillis() {
        Budget b = local.get();
        return b == null ? properties.getRequestDeadlineMillis() : Math.max(0, b.limitMillis - elapsed(b));
    }
    public boolean claimRetry() {
        Budget b = required();
        if (b.retriesUsed >= 1) return false;
        b.retriesUsed++; return true;
    }
    public boolean canRetry() { return required().retriesUsed < 1; }
    public void retryDecision(Stage stage, String value) { state(stage).retryDecision = value; }
    public void attempt(Stage stage, String model, boolean retry) {
        var s = state(stage); s.model = model; s.attempts++; if (retry) s.retries++;
    }
    public void record(Stage stage, String source, String method, String reason, Integer status) {
        var s = state(stage); s.source = source; s.method = method; s.reason = reason;
        if (status != null) s.status = status;
    }
    public void failure(Stage stage, String reason, Integer status) {
        record(stage, "UNAVAILABLE", "NONE", reason, status);
    }
    public String lastReason(Stage stage) { return state(stage).reason; }
    public Report report() {
        Budget b = required();
        var stages = Arrays.stream(Stage.values()).map(stage -> {
            var s = b.stages.get(stage);
            return new StageResult(stage, s.source, s.method, s.model, s.attempts, s.retries, s.reason, s.status, s.retryDecision);
        }).toList();
        boolean ai = stages.stream().anyMatch(s -> s.source().equals("AI"));
        boolean fallback = stages.stream().anyMatch(s -> s.source().equals("FALLBACK"));
        boolean input = stages.stream().anyMatch(s -> s.source().equals("USER_PROVIDED"));
        String overall = ai ? fallback ? "AI_WITH_FALLBACK" : "AI_ASSISTED"
                : fallback ? "FALLBACK" : input ? "USER_PROVIDED" : "UNAVAILABLE";
        return new Report(overall, b.limitMillis, elapsed(b), b.retriesUsed, stages);
    }
    private long elapsed(Budget b) { return Math.max(0, (nanoTime.getAsLong() - b.startNanos) / 1_000_000); }
    private Budget required() {
        Budget b = local.get(); if (b == null) throw new IllegalStateException("No AI request scope"); return b;
    }
    private MutableStage state(Stage stage) { return required().stages.get(stage); }
    private static class MutableStage {
        String source = "SKIPPED", method = "NONE", model, reason, retryDecision = "NONE"; Integer status; int attempts, retries;
    }
    private static class Budget {
        final long startNanos, limitMillis; int retriesUsed;
        final EnumMap<Stage, MutableStage> stages = new EnumMap<>(Stage.class);
        Budget(long start, long limit) { startNanos = start; limitMillis = limit; for (var s : Stage.values()) stages.put(s, new MutableStage()); }
    }
    public final class Scope implements AutoCloseable {
        private final boolean owner;
        private Scope(boolean owner) { this.owner = owner; }
        @Override public void close() { if (owner) local.remove(); }
    }
}
