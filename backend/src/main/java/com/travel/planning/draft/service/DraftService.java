package com.travel.planning.draft.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.place.entity.Place;
import com.travel.place.repository.PlaceRepository;
import com.travel.planning.ai.intent.dto.response.IntentAnalysisResponse;
import com.travel.planning.ai.intent.dto.response.IntentSearchResponse;
import com.travel.planning.ai.intent.model.TravelIntent;
import com.travel.planning.ai.intent.service.TravelIntentService;
import com.travel.planning.ai.resilience.AiCallFailure;
import com.travel.planning.ai.resilience.AiRequestContext;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.dto.response.DraftResponse;
import com.travel.planning.retrieval.config.RagProperties;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;
import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.retrieval.service.PlaceRetrievalService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.travel.planning.ai.resilience.AiRequestContext.Stage.*;
import static com.travel.planning.draft.dto.response.DraftResponse.*;

@Service
@Profile({"dev", "rag-demo"})
public class DraftService {

    private final TravelIntentService intents;
    private final PlaceRetrievalService retrieval;
    private final PlaceRepository places;
    private final DraftAllocationService allocations;
    private final DraftSchedulingService scheduling;
    private final DraftThemeService themes;
    private final DraftWeatherService weather;
    private final RagProperties properties;
    private final AiRequestContext context;

    public DraftService(TravelIntentService intents, PlaceRetrievalService retrieval, PlaceRepository places,
                        DraftAllocationService allocations, DraftSchedulingService scheduling, DraftThemeService themes,
                        DraftWeatherService weather, RagProperties properties, AiRequestContext context) {
        this.intents = intents;
        this.retrieval = retrieval;
        this.places = places;
        this.allocations = allocations;
        this.scheduling = scheduling;
        this.themes = themes;
        this.weather = weather;
        this.properties = properties;
        this.context = context;
    }

    public DraftResponse generate(GenerateDraftRequest request) {
        try (var scope = context.ensure()) {
            return generateWithinBudget(request);
        }
    }

    private DraftResponse generateWithinBudget(GenerateDraftRequest request) {
        Map<String, String> metadata = metadata(request);
        List<Problem> issues = new ArrayList<>();
        IntentAnalysisResponse analysis;
        try {
            analysis = context.measure(INTENT, () -> request.preferences() == null
                    ? intents.analyze(request.text()) : intents.analyze(request.preferences().toIntent()));
            context.record(INTENT, request.preferences() == null ? "AI" : "USER_PROVIDED",
                    request.preferences() == null ? "GEMINI_STRUCTURED_EXTRACTION" : "STRUCTURED_PREFERENCES", context.lastReason(INTENT), null);
        } catch (AppException ex) {
            if (request.preferences() != null) throw ex;
            String reason = ex instanceof AiCallFailure failure ? failure.reason()
                    : ex.getErrorCode() == ErrorCode.AI_VALIDATION_FAILED ? "INVALID_RESPONSE" : "AI_UNAVAILABLE";
            context.failure(INTENT, reason, ex instanceof AiCallFailure failure ? failure.providerStatus() : null);
            metadata.put("nextAction", "RESUBMIT_WITH_PREFERENCES");
            issues.add(new Problem("INTENT_UNAVAILABLE", true, null,
                    "Chưa đọc được yêu cầu bằng AI (" + reason + "). Hãy gửi preferences có cấu trúc thay cho text; hệ thống không tự đoán yêu cầu."));
            return response("NEEDS_STRUCTURED_INPUT", request, null, List.of(), List.of(), issues, DraftWeatherService.unknown(), metadata);
        }
        var initial = new IntentSearchResponse(analysis.status(), analysis, null);
        if (!"READY_FOR_RETRIEVAL".equals(analysis.status())) {
            analysis.questions().forEach(q -> issues.add(new Problem("NEEDS_INPUT", true, null, q)));
            return response(analysis.status(), request, initial, List.of(), List.of(), issues, DraftWeatherService.unknown(), metadata);
        }
        validatePlanningInput(request, analysis.intent(), issues);
        if (!issues.isEmpty())
            return response("NEEDS_INPUT", request, initial, List.of(), List.of(), issues, DraftWeatherService.unknown(), metadata);

        // Reject incomplete planning input before paying for a query embedding.
        var search = context.measure(RETRIEVAL, () -> intents.search(analysis, 8));
        if (search.candidates() != null) {
            var result = search.candidates();
            boolean hybrid = "HYBRID".equals(result.retrievalMode());
            context.record(RETRIEVAL, hybrid ? "AI" : "FALLBACK", hybrid ? "HYBRID_RRF" : "LEXICAL_DATABASE_SEARCH",
                    hybrid ? context.lastReason(RETRIEVAL) : result.fallbackReason(), null);
        }
        if (!"CANDIDATES_FOUND".equals(search.status())) {
            search.analysis().questions().forEach(q -> issues.add(new Problem("NEEDS_INPUT", true, null, q)));
            return response(search.status(), request, search, List.of(), List.of(), issues, DraftWeatherService.unknown(), metadata);
        }
        Map<UUID, PlaceCandidate> current = retrieval.catalog().stream().collect(Collectors.toMap(PlaceCandidate::placeId, Function.identity()));
        List<PlaceCandidate> candidates = search.candidates().matches().stream().map(PlaceSearchResponse.Match::place)
                .map(p -> current.get(p.placeId())).filter(Objects::nonNull).toList();
        Set<UUID> required = analysis.mustVisitPlaces().stream().map(PlaceCandidate::placeId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Place> entities = places.findAllByIdIn(candidates.stream().map(PlaceCandidate::placeId).toList()).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        candidates = candidates.stream().filter(p -> entities.containsKey(p.placeId())).toList();
        Set<UUID> allowed = candidates.stream().map(PlaceCandidate::placeId).collect(Collectors.toSet());
        if (!allowed.containsAll(required)) {
            issues.add(new Problem("STALE_REQUIRED_PLACE", true, null, "Điểm bắt buộc đã thay đổi hoặc không còn được duyệt; hãy tạo lại yêu cầu."));
            return response("NEEDS_INPUT", request, search, List.of(), List.of(), issues, DraftWeatherService.unknown(), metadata);
        }
        if (candidates.isEmpty())
            return response("NO_CANDIDATES", request, search, List.of(), List.of(), issues, DraftWeatherService.unknown(), metadata);
        TravelIntent intent = analysis.intent();
        int requiredMinutes = candidates.stream().filter(p -> required.contains(p.placeId())).mapToInt(PlaceCandidate::estimatedDurationMinutes).sum();
        long availableMinutes = Duration.between(LocalTime.parse(intent.dailyStartTime()), LocalTime.parse(intent.dailyEndTime())).toMinutes() * intent.numberOfDays();
        if (requiredMinutes > availableMinutes) {
            issues.add(new Problem("REQUIRED_TIME_LOWER_BOUND", true, null, "Tổng thời lượng điểm bắt buộc đã vượt quỹ giờ, ngay cả khi chưa tính di chuyển/nghỉ. Cần đổi yêu cầu."));
            return response("INFEASIBLE", request, search, List.of(), candidates.stream().map(p -> new Unassigned(p, "REQUIRED_TIME_LOWER_BOUND")).toList(), issues, DraftWeatherService.unknown(), metadata);
        }
        return complete(request, search, candidates, entities, required, issues, metadata);
    }

    private DraftResponse complete(GenerateDraftRequest request, IntentSearchResponse search, List<PlaceCandidate> candidates,
                                   Map<UUID, Place> entities, Set<UUID> required, List<Problem> issues, Map<String, String> metadata) {
        TravelIntent intent = search.analysis().intent();
        metadata.put("bufferMinutesBetweenStops", Integer.toString(scheduling.bufferMinutes(intent)));
        if (intent.travelPace() == null)
            issues.add(new Problem("PACE_NOT_SPECIFIED", false, null, "Nhịp độ chưa nêu; sử dụng cấu hình demo cân bằng, không ghi thành sở thích người dùng."));
        try (var forecast = weather.start(request, intent.numberOfDays())) {
            var allocation = context.measure(ALLOCATION, () -> allocations.allocate(candidates, required, intent, request.startDate()));
            boolean aiAllocation = "AI".equals(allocation.source());
            context.record(ALLOCATION, aiAllocation ? "AI" : "FALLBACK", aiAllocation ? "GEMINI_VALIDATED_PLACE_REFS" : "RULE_BASED_LOAD_BALANCING",
                    aiAllocation ? context.lastReason(ALLOCATION) : allocation.reason(), null);
            metadata.put("allocationSource", allocation.source());
            if (!aiAllocation)
                issues.add(new Problem("ALLOCATION_FALLBACK", false, null, "Không sử dụng được đề xuất phân bổ AI; backend phân bổ cơ bản từ dữ liệu thật."));

            var schedule = context.measure(OPTIMIZATION, () -> scheduling.schedule(request, intent, allocation, candidates, entities, required));
            context.record(OPTIMIZATION, "DETERMINISTIC", "HELD_KARP_AND_TIMELINE_WITH_BOUNDED_REPAIR", null, null);
            metadata.put("repairPasses", Integer.toString(schedule.repairPasses()));
            if (schedule.days().stream().allMatch(d -> d.stops().isEmpty())) {
                issues.add(new Problem("NO_SCHEDULED_PLACES", true, null, "Chưa xếp được điểm phù hợp với yêu cầu trong kho demo; hãy điều chỉnh sở thích hoặc điều kiện loại trừ."));
                context.record(WEATHER, "UNAVAILABLE", "CANCELLED", "NO_SCHEDULED_PLACES", null);
                return response("NO_CANDIDATES", request, search, List.of(), schedule.unassigned(), issues, DraftWeatherService.unknown(), metadata);
            }
            var narration = context.measure(THEMES, () -> themes.narrate(schedule.days(), allocation));
            context.record(THEMES, narration.source(), "ALLOCATION_HINTS_AND_GROUNDED_TEMPLATE", narration.fallbackReason(), null);
            if (!"FALLBACK".equals(narration.source())) context.reuseModel(THEMES, ALLOCATION);
            if (!"AI".equals(narration.source()))
                issues.add(new Problem("THEME_FALLBACK", false, null, "Một hoặc nhiều chủ đề/tóm tắt dùng mẫu từ lịch cuối vì gợi ý AI không khả dụng hoặc không còn hợp lệ."));
            narration.days().forEach(d -> issues.addAll(d.issues()));
            issues.add(new Problem("ESTIMATED_ROUTE", false, null, "Quãng đường là Haversine và thời gian theo 35 km/h; chưa phải định tuyến đường bộ hay giao thông thực tế."));

            var weatherResult = forecast.await();
            boolean available = "AVAILABLE_ORIGIN_ONLY".equals(weatherResult.advice().status());
            context.elapsedMillis(WEATHER, weatherResult.elapsedMillis());
            context.record(WEATHER, available ? "EXTERNAL_API" : "UNAVAILABLE", available ? "OPEN_METEO_ORIGIN_ONLY" : "NONE", weatherResult.reason(), null);
            issues.add(new Problem("WEATHER_ADVISORY", false, null, available
                    ? "Thời tiết chỉ tham khảo tại tọa độ xuất phát; chưa xác minh theo từng điểm/khung tham quan, không bảo đảm an toàn hoạt động."
                    : "Chưa có dự báo thật đầy đủ; không dùng dữ liệu thời tiết mô phỏng để đánh giá lịch."));
            return response(issues.stream().anyMatch(Problem::blocking) ? "CONFLICTS_DETECTED" : "READY_WITH_WARNINGS",
                    request, search, narration.days(), schedule.unassigned(), issues, weatherResult.advice(), metadata);
        }
    }

    private void validatePlanningInput(GenerateDraftRequest request, TravelIntent intent, List<Problem> issues) {
        if (intent.numberOfDays() == null || intent.numberOfDays() < 1 || intent.numberOfDays() > 2)
            issues.add(new Problem("NUMBER_OF_DAYS", true, null, "Hãy nêu rõ chuyến đi một hoặc hai ngày trong yêu cầu."));
        if (intent.dailyStartTime() == null || intent.dailyEndTime() == null)
            issues.add(new Problem("DAILY_WINDOW", true, null, "Hãy nêu giờ bắt đầu và kết thúc mỗi ngày; hệ thống không tự đoán."));
        if (request.origin().latitude() < 15.8 || request.origin().latitude() > 16.3 || request.origin().longitude() < 107.9 || request.origin().longitude() > 108.4)
            issues.add(new Problem("ORIGIN_OUTSIDE_DEMO", true, null, "Điểm xuất phát cần nằm trong khu vực demo Đà Nẵng."));
    }

    private Map<String, String> metadata(GenerateDraftRequest request) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("model", properties.getIntentModel());
        metadata.put("routeSource", "ESTIMATED_HAVERSINE_35_KMH");
        metadata.put("optimizationScope", "HELD_KARP_PER_DAY_DISTANCE_ONLY");
        metadata.put("persistence", "PREVIEW_ONLY");
        metadata.put("originSource", "USER_PROVIDED_ANCHOR_NOT_DB_PLACE");
        metadata.put("restPolicy", request.restMinutes() + " phút nghỉ sau hai điểm nếu còn điểm tiếp theo; đệm theo nhịp độ.");
        return metadata;
    }

    private DraftResponse response(String status, GenerateDraftRequest request, IntentSearchResponse search, List<Day> days,
                                   List<Unassigned> unassigned, List<Problem> issues, WeatherAdvice forecast, Map<String, String> metadata) {
        return new DraftResponse(status, request.startDate(), "Asia/Ho_Chi_Minh", search == null ? null : search.analysis(),
                search == null ? null : search.candidates(), List.copyOf(days), unassigned, List.copyOf(issues), forecast, Map.copyOf(metadata), context.report());
    }
}

