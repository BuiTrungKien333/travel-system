package com.travel.planning.draft;

import com.travel.place.entity.Place;
import com.travel.place.repository.PlaceRepository;
import com.travel.planning.ai.intent.TravelIntentService;
import com.travel.planning.ai.resilience.*;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.planning.retrieval.*;
import com.travel.weather.service.WeatherService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.travel.planning.draft.DraftModels.*;
import static com.travel.planning.ai.resilience.AiRequestContext.Stage.*;

@Service
@Profile("rag-demo")
public class DraftService {
    private final TravelIntentService intents;
    private final PlaceRetrievalService retrieval;
    private final PlaceRepository places;
    private final DraftAiAdvisor advisor;
    private final DraftTimelinePlanner planner;
    private final WeatherService weather;
    private final RagDemoProperties properties;
    private final AiRequestContext context;

    @Autowired
    public DraftService(TravelIntentService intents, PlaceRetrievalService retrieval, PlaceRepository places,
                        DraftAiAdvisor advisor, DraftTimelinePlanner planner, WeatherService weather, RagDemoProperties properties,
                        AiRequestContext context) {
        this.intents = intents;
        this.retrieval = retrieval;
        this.places = places;
        this.advisor = advisor;
        this.planner = planner;
        this.weather = weather;
        this.properties = properties;
        this.context = context;
    }

    public DraftService(TravelIntentService intents, PlaceRetrievalService retrieval, PlaceRepository places,
                        DraftAiAdvisor advisor, DraftTimelinePlanner planner, WeatherService weather, RagDemoProperties properties) {
        this(intents, retrieval, places, advisor, planner, weather, properties, new AiRequestContext(properties));
    }

    public Response generate(DraftController.Request request) {
        try (var scope = context.ensure()) {
            return generateWithinBudget(request);
        }
    }

    private Response generateWithinBudget(DraftController.Request request) {
        TravelIntentService.TextSearchResult search;
        try {
            if (request.preferences() != null) {
                context.record(INTENT, "USER_PROVIDED", "STRUCTURED_PREFERENCES", null, null);
                search = intents.search(request.preferences().toIntent(), 8);
            } else {
                search = intents.search(request.text(), 8);
                context.record(INTENT, "AI", "GEMINI_STRUCTURED_EXTRACTION", context.lastReason(INTENT), null);
            }
        } catch (AppException ex) {
            if (request.preferences() != null) throw ex;
            String reason = ex instanceof AiCallFailure failure ? failure.reason()
                    : ex.getErrorCode() == ErrorCode.AI_VALIDATION_FAILED ? "INVALID_RESPONSE" : "AI_UNAVAILABLE";

            context.failure(INTENT, reason, ex instanceof AiCallFailure failure ? failure.providerStatus() : null);
            return response("NEEDS_STRUCTURED_INPUT", request, null, List.of(), List.of(),
                    List.of(new Problem("INTENT_UNAVAILABLE", true, null,
                            "Chưa đọc được yêu cầu bằng AI (" + reason + "). Hãy gửi preferences có cấu trúc thay cho text; hệ thống không tự đoán yêu cầu.")),
                    unknownWeather(), Map.of("persistence", "PREVIEW_ONLY", "nextAction", "RESUBMIT_WITH_PREFERENCES"));
        }
        if (search.candidates() != null) {
            var result = search.candidates();
            boolean hybrid = "HYBRID".equals(result.retrievalMode());
            context.record(RETRIEVAL, hybrid ? "AI" : "FALLBACK", hybrid ? "HYBRID_RRF" : "LEXICAL_DATABASE_SEARCH",
                    hybrid ? context.lastReason(RETRIEVAL) : result.fallbackReason(), null);
        }

        var analysis = search.analysis();
        var issues = new ArrayList<Problem>();
        var metadata = new LinkedHashMap<String, String>();
        metadata.put("model", properties.getIntentModel());
        metadata.put("routeSource", "ESTIMATED_HAVERSINE_35_KMH");
        metadata.put("optimizationScope", "HELD_KARP_PER_DAY_DISTANCE_ONLY");
        metadata.put("persistence", "PREVIEW_ONLY");
        metadata.put("originSource", "USER_PROVIDED_ANCHOR_NOT_DB_PLACE");
        metadata.put("restPolicy", request.restMinutes() + " phút nghỉ sau hai điểm nếu còn điểm tiếp theo; đệm theo nhịp độ.");
        if (!"CANDIDATES_FOUND".equals(search.status())) {
            analysis.questions().forEach(q -> issues.add(new Problem("NEEDS_INPUT", true, null, q)));
            return response(search.status(), request, search, List.of(), List.of(), issues, unknownWeather(), metadata);
        }
        var intent = analysis.intent();
        if (intent.numberOfDays() == null || intent.numberOfDays() < 1 || intent.numberOfDays() > 2)
            issues.add(new Problem("NUMBER_OF_DAYS", true, null, "Hãy nêu rõ chuyến đi một hoặc hai ngày trong câu yêu cầu."));

        if (intent.dailyStartTime() == null || intent.dailyEndTime() == null)
            issues.add(new Problem("DAILY_WINDOW", true, null, "Hãy nêu giờ bắt đầu và kết thúc mỗi ngày; hệ thống không tự đoán."));

        if (request.origin().latitude() < 15.8 || request.origin().latitude() > 16.3
                || request.origin().longitude() < 107.9 || request.origin().longitude() > 108.4)
            issues.add(new Problem("ORIGIN_OUTSIDE_DEMO", true, null, "Điểm xuất phát cần nằm trong khu vực demo Đà Nẵng."));

        if (!issues.isEmpty())
            return response("NEEDS_INPUT", request, search, List.of(), List.of(), issues, unknownWeather(), metadata);

        Map<UUID, PlaceCandidate> current = retrieval.catalog().stream().collect(Collectors.toMap(PlaceCandidate::placeId, Function.identity()));
        List<PlaceCandidate> candidates = search.candidates().matches().stream().map(PlaceRetrievalService.Match::place)
                .map(p -> current.get(p.placeId())).filter(Objects::nonNull).toList();

        Set<UUID> required = analysis.mustVisitPlaces().stream().map(PlaceCandidate::placeId).collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, Place> entities = places.findAllByIdIn(candidates.stream().map(PlaceCandidate::placeId).toList()).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));

        candidates = candidates.stream().filter(p -> entities.containsKey(p.placeId())).toList();
        Set<UUID> allowed = candidates.stream().map(PlaceCandidate::placeId).collect(Collectors.toSet());
        if (!allowed.containsAll(required)) {
            issues.add(new Problem("STALE_REQUIRED_PLACE", true, null, "Điểm bắt buộc đã thay đổi hoặc không còn được duyệt; hãy tạo lại yêu cầu."));
            return response("NEEDS_INPUT", request, search, List.of(), List.of(), issues, unknownWeather(), metadata);
        }

        if (candidates.isEmpty())
            return response("NO_CANDIDATES", request, search, List.of(), List.of(), issues, unknownWeather(), metadata);
        int days = intent.numberOfDays();

        LocalTime start = LocalTime.parse(intent.dailyStartTime()), end = LocalTime.parse(intent.dailyEndTime());
        int requiredMinutes = candidates.stream().filter(p -> required.contains(p.placeId())).mapToInt(PlaceCandidate::estimatedDurationMinutes).sum();
        if (requiredMinutes > Duration.between(start, end).toMinutes() * days) {
            issues.add(new Problem("REQUIRED_TIME_LOWER_BOUND", true, null, "Tổng thời lượng điểm bắt buộc đã vượt quỹ giờ, ngay cả khi chưa tính di chuyển/nghỉ. Cần đổi yêu cầu."));
            return response("INFEASIBLE", request, search, List.of(), candidates.stream().map(p -> new Unassigned(p, "REQUIRED_TIME_LOWER_BOUND")).toList(), issues, unknownWeather(), metadata);
        }

        int buffer = "RELAXED".equals(intent.travelPace()) ? 15 : "ACTIVE".equals(intent.travelPace()) ? 5 : 10;
        metadata.put("bufferMinutesBetweenStops", Integer.toString(buffer));

        if (intent.travelPace() == null)
            issues.add(new Problem("PACE_NOT_SPECIFIED", false, null, "Nhịp độ chưa nêu; sử dụng cấu hình demo cân bằng, không ghi thành sở thích người dùng."));

        var allocation = advisor.allocate(candidates, required, intent, days, request.startDate());
        context.record(ALLOCATION, allocation.source().equals("AI") ? "AI" : "FALLBACK",
                allocation.source().equals("AI") ? "GEMINI_VALIDATED_PLACE_REFS" : "RULE_BASED_LOAD_BALANCING",
                allocation.source().equals("AI") ? context.lastReason(ALLOCATION) : allocation.reason(), null);

        metadata.put("allocationSource", allocation.source());
        if (allocation.warning() != null)
            issues.add(new Problem("ALLOCATION_FALLBACK", false, null, allocation.warning()));

        List<List<UUID>> groups = allocation.days().stream().map(ArrayList::new).collect(Collectors.toCollection(ArrayList::new));
        Map<UUID, PlaceCandidate> byId = candidates.stream().collect(Collectors.toMap(PlaceCandidate::placeId, Function.identity()));
        List<Day> built = build(groups, request, start, end, buffer, byId, entities);
        Map<UUID, String> removed = new HashMap<>();
        int repairPasses = 0;
        // Bounded deterministic repair; never drops a required place or calls the model again.
        for (int pass = 0; pass < 2; pass++) {
            int bad = -1;
            for (int i = 0; i < built.size(); i++)
                if (built.get(i).blocked()) {
                    bad = i;
                    break;
                }
            if (bad == -1) break;
            Day badDay = built.get(bad);
            UUID target = badDay.issues().stream().filter(Problem::blocking).map(Problem::placeId).filter(Objects::nonNull).findFirst().orElse(null);
            if (target == null)
                target = groups.get(bad).stream().filter(id -> !required.contains(id)).reduce((a, b) -> b)
                        .orElse(groups.get(bad).getLast());
            boolean moved = false;
            for (int other = 0; other < groups.size(); other++) {
                if (other == bad) continue;
                var trial = copy(groups);
                trial.get(bad).remove(target);
                trial.get(other).add(target);
                var next = build(trial, request, start, end, buffer, byId, entities);
                if (!next.get(other).blocked() && next.get(bad).issues().stream().filter(Problem::blocking).count()
                        < badDay.issues().stream().filter(Problem::blocking).count()) {
                    groups = trial;
                    built = next;
                    moved = true;
                    break;
                }
            }
            if (!moved) {
                if (required.contains(target)) break;
                groups.get(bad).remove(target);
                removed.put(target, "REMOVED_TO_RESOLVE_CONFLICT");
                built = build(groups, request, start, end, buffer, byId, entities);
            }
            repairPasses++;
        }

        metadata.put("repairPasses", Integer.toString(repairPasses));
        context.record(OPTIMIZATION, "DETERMINISTIC", "HELD_KARP_AND_TIMELINE_WITH_BOUNDED_REPAIR", null, null);
        Set<UUID> assigned = built.stream().flatMap(d -> d.stops().stream()).map(s -> s.place().placeId()).collect(Collectors.toSet());
        if (!assigned.containsAll(required)) throw new IllegalStateException("Draft lost a required place");
        var unassigned = candidates.stream().filter(p -> !assigned.contains(p.placeId()))
                .map(p -> new Unassigned(p, removed.getOrDefault(p.placeId(), "NOT_SELECTED"))).toList();
        if (assigned.isEmpty()) {
            issues.add(new Problem("NO_SCHEDULED_PLACES", true, null, "Chưa xếp được điểm phù hợp với yêu cầu trong kho demo; hãy điều chỉnh sở thích hoặc điều kiện loại trừ."));
            return response("NO_CANDIDATES", request, search, List.of(), unassigned, issues, unknownWeather(), metadata);
        }

        var narration = advisor.narrateWithProvenance(built);
        built = narration.days();
        boolean aiThemes = built.stream().allMatch(d -> "AI_GROUNDED_TEMPLATE".equals(d.summarySource()));
        context.record(THEMES, aiThemes ? "AI" : "FALLBACK",
                aiThemes ? "GEMINI_THEME_SELECTION_BACKEND_TEMPLATE" : "RULE_BASED_TEMPLATE",
                aiThemes ? context.lastReason(THEMES) : narration.fallbackReason(), null);
        if (built.stream().anyMatch(d -> d.summarySource().equals("RULE_BASED_FALLBACK")))
            issues.add(new Problem("THEME_FALLBACK", false, null, "Chủ đề/tóm tắt được dựng bằng mẫu từ lịch cuối vì AI không khả dụng hoặc kết quả không hợp lệ."));
        built.forEach(d -> issues.addAll(d.issues()));

        issues.add(new Problem("ESTIMATED_ROUTE", false, null, "Quãng đường là Haversine và thời gian theo 35 km/h; chưa phải định tuyến đường bộ hay giao thông thực tế."));
        var forecast = weather(request, days);
        context.record(WEATHER, forecast.status().equals("AVAILABLE_ORIGIN_ONLY") ? "EXTERNAL_API" : "UNAVAILABLE",
                forecast.status().equals("AVAILABLE_ORIGIN_ONLY") ? "OPEN_METEO_ORIGIN_ONLY" : "NONE",
                forecast.status().equals("AVAILABLE_ORIGIN_ONLY") ? null : "REAL_FORECAST_UNAVAILABLE", null);

        issues.add(new Problem("WEATHER_ADVISORY", false, null, forecast.status().equals("AVAILABLE_ORIGIN_ONLY")
                ? "Thời tiết chỉ tham khảo tại tọa độ xuất phát; chưa xác minh theo từng điểm/khung tham quan, không bảo đảm an toàn hoạt động."
                : "Chưa có dự báo thật đầy đủ; không dùng dữ liệu thời tiết mô phỏng để đánh giá lịch."));

        return response(issues.stream().anyMatch(Problem::blocking) ? "CONFLICTS_DETECTED" : "READY_WITH_WARNINGS",
                request, search, built, unassigned, issues, forecast, metadata);
    }

    private List<Day> build(List<List<UUID>> groups, DraftController.Request request, LocalTime start, LocalTime end,
                            int buffer, Map<UUID, PlaceCandidate> candidates, Map<UUID, Place> entities) {
        var result = new ArrayList<Day>();
        for (int i = 0; i < groups.size(); i++)
            result.add(planner.build(i + 1, request.startDate().plusDays(i), start, end,
                    request.origin(), request.roundTrip(), request.restMinutes(), buffer, groups.get(i).stream().map(candidates::get).toList(), entities));

        return result;
    }

    private List<List<UUID>> copy(List<List<UUID>> groups) {
        return groups.stream().map(g -> (List<UUID>) new ArrayList<>(g)).collect(Collectors.toCollection(ArrayList::new));
    }

    private Response response(String status, DraftController.Request request, TravelIntentService.TextSearchResult search,
                              List<Day> days, List<Unassigned> unassigned, List<Problem> issues, WeatherAdvice forecast, Map<String, String> metadata) {

        return new Response(status, request.startDate(), "Asia/Ho_Chi_Minh", search == null ? null : search.analysis(), search == null ? null : search.candidates(),
                List.copyOf(days), unassigned, List.copyOf(issues), forecast, Map.copyOf(metadata), context.report());
    }

    private WeatherAdvice unknownWeather() {
        return new WeatherAdvice("UNKNOWN", "UNAVAILABLE", "ORIGIN_COORDINATES_ONLY", List.of());
    }

    private WeatherAdvice weather(DraftController.Request request, int days) {
        try {
            var result = weather.getForecast(request.origin().latitude(), request.origin().longitude(), request.startDate(), request.startDate().plusDays(days - 1));
            if (result == null || !"open-meteo".equals(result.providerName())) return unknownWeather();
            var summaries = result.dailySummaries().stream().filter(d -> !d.date().isBefore(request.startDate()) && d.date().isBefore(request.startDate().plusDays(days))).toList();
            if (summaries.size() != days) return unknownWeather();

            return new WeatherAdvice("AVAILABLE_ORIGIN_ONLY", result.providerName(), "ORIGIN_COORDINATES_ONLY", summaries.stream()
                    .map(d -> new WeatherDay(d.date(), d.minTemperature(), d.maxTemperature(), d.maxPrecipitationProbability(),
                            d.dominantCondition() == null ? "UNKNOWN" : d.dominantCondition().name(),
                            d.highestRiskLevel() == null ? "UNKNOWN" : d.highestRiskLevel().name())).toList());
        } catch (Exception ex) {
            return unknownWeather();
        }
    }
}
