package com.travel.planning.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.place.entity.Place;
import com.travel.place.enums.PlaceType;
import com.travel.place.model.WeeklyOpeningHours;
import com.travel.place.repository.PlaceRepository;
import com.travel.planning.ai.copilot.enums.CopilotActionType;
import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;
import com.travel.planning.ai.provider.AiProvider;
import com.travel.planning.ai.provider.AiProviderFactory;
import com.travel.planning.algorithm.DistanceCalculator;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.algorithm.model.TspRequest;
import com.travel.planning.algorithm.model.TspResult;
import com.travel.planning.dto.request.ApplyRecommendationRequest;
import com.travel.planning.dto.request.DayPlanRequest;
import com.travel.planning.dto.request.MultiDayOptimizeAndAuditRequest;
import com.travel.planning.dto.request.OptimizeAndAuditRequest;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.dto.response.DayScheduleResponse;
import com.travel.planning.dto.response.MultiDayOptimizeAndAuditResponse;
import com.travel.planning.dto.response.TripOptimizationResponse;
import com.travel.planning.enums.VisitStatus;
import com.travel.planning.rule.checker.TimelineCollisionRuleChecker;
import com.travel.planning.rule.model.DetectedIssue;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.planning.rule.service.RuleEvaluatorService;
import com.travel.planning.timeline.TimelineEngine;
import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.WeatherForecastResult;
import com.travel.weather.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j(topic = "TRIP_OPTIMIZATION_SERVICE")
public class TripOptimizationServiceImpl implements TripOptimizationService {

    private final HeldKarpSolver heldKarpSolver;
    private final WeatherService weatherService;
    private final RuleEvaluatorService ruleEvaluatorService;
    private final AiProviderFactory aiProviderFactory;
    private final PlaceRepository placeRepository;
    private final TimelineEngine timelineEngine;
    private final TimelineCollisionRuleChecker timelineCollisionRuleChecker;
    private final ObjectMapper objectMapper;

    @Autowired
    public TripOptimizationServiceImpl(
            HeldKarpSolver heldKarpSolver,
            WeatherService weatherService,
            RuleEvaluatorService ruleEvaluatorService,
            AiProviderFactory aiProviderFactory,
            PlaceRepository placeRepository,
            TimelineEngine timelineEngine,
            TimelineCollisionRuleChecker timelineCollisionRuleChecker,
            ObjectMapper objectMapper
    ) {
        this.heldKarpSolver = heldKarpSolver;
        this.weatherService = weatherService;
        this.ruleEvaluatorService = ruleEvaluatorService;
        this.aiProviderFactory = aiProviderFactory;
        this.placeRepository = placeRepository;
        this.timelineEngine = timelineEngine;
        this.timelineCollisionRuleChecker = timelineCollisionRuleChecker;
        this.objectMapper = objectMapper;
    }

    public TripOptimizationServiceImpl(
            HeldKarpSolver heldKarpSolver,
            WeatherService weatherService,
            RuleEvaluatorService ruleEvaluatorService,
            AiProviderFactory aiProviderFactory,
            PlaceRepository placeRepository
    ) {
        this(heldKarpSolver, weatherService, ruleEvaluatorService, aiProviderFactory, placeRepository,
                new TimelineEngine(), new TimelineCollisionRuleChecker(), new ObjectMapper());
    }

    @Override
    public TripOptimizationResponse optimizeAndAuditItinerary(OptimizeAndAuditRequest request) {
        log.info("[TRIP_OPTIMIZER] Starting optimization & audit pipeline for destination='{}', {} locations, date={}, hasOrigin={}",
                request.destination(), request.locations().size(), request.tripDate(), request.originPlace() != null);

        Map<UUID, TspLocationRequest> requestMap = new HashMap<>();
        List<Place> visitPlaces = new ArrayList<>();

        // 1. Chuyển đổi DTO sang List<Place> và tra cứu CSDL (Place) nếu có
        for (TspLocationRequest loc : request.locations()) {
            UUID placeId = loc.id() != null ? loc.id() : UUID.randomUUID();
            requestMap.put(placeId, loc);

            PlaceType placeType = loc.placeType() != null ? loc.placeType() : PlaceType.UNSPECIFIED;
            LocalTime open = loc.openingTime();
            LocalTime close = loc.closingTime();
            WeeklyOpeningHours weeklyHours = loc.weeklyOpeningHours();

            if (loc.id() != null) {
                Optional<Place> dbPlaceOpt = placeRepository.findById(loc.id());
                if (dbPlaceOpt.isPresent()) {
                    Place dbPlace = dbPlaceOpt.get();
                    if (placeType == PlaceType.UNSPECIFIED && dbPlace.getPlaceType() != null) {
                        placeType = dbPlace.getPlaceType();
                    }
                    if (open == null && dbPlace.getOpeningTime() != null) {
                        open = dbPlace.getOpeningTime();
                    }
                    if (close == null && dbPlace.getClosingTime() != null) {
                        close = dbPlace.getClosingTime();
                    }
                    if (weeklyHours == null && dbPlace.getWeeklyOpeningHours() != null) {
                        try {
                            weeklyHours = objectMapper.readValue(dbPlace.getWeeklyOpeningHours(), WeeklyOpeningHours.class);
                        } catch (Exception ex) {
                            log.warn("[TRIP_OPTIMIZER] Failed to parse weeklyOpeningHours JSON for place {}: {}", loc.id(), ex.getMessage());
                        }
                    }
                }
            }

            visitPlaces.add(Place.builder()
                    .id(placeId)
                    .name(loc.name())
                    .latitude(loc.latitude())
                    .longitude(loc.longitude())
                    .placeType(placeType)
                    .openingTime(open)
                    .closingTime(close)
                    .build());
        }

        // 2. Xử lý In-progress Re-optimization hoặc Full Optimization
        List<Place> orderedPlaces;
        List<com.travel.planning.algorithm.model.RouteLeg> legs = null;

        boolean hasProgress = request.locations().stream()
                .anyMatch(l -> l.visitStatus() == VisitStatus.COMPLETED || l.visitStatus() == VisitStatus.IN_PROGRESS);

        if (hasProgress) {
            orderedPlaces = solveInProgressReOptimization(request, visitPlaces, requestMap);
        } else if (request.originPlace() != null) {
            // Có điểm neo xuất phát cố định (khách sạn/vị trí hiện tại):
            TspResult tspResult = solveWithFixedOrigin(request, visitPlaces);
            UUID originId = request.originPlace().id();
            orderedPlaces = extractVisitPlaces(tspResult.getOrderedPlaces(), originId);
            legs = tspResult.getLegs();
        } else {
            // Không có điểm neo xuất phát riêng: Tối ưu tự do hoặc theo startPlaceId
            TspRequest tspRequest = TspRequest.builder()
                    .places(visitPlaces)
                    .startPlaceId(request.startPlaceId())
                    .roundTrip(request.isRoundTrip())
                    .build();
            TspResult tspResult = heldKarpSolver.solve(tspRequest);
            orderedPlaces = tspResult.getOrderedPlaces();
            legs = tspResult.getLegs();
        }

        // 3. Xây dựng trục thời gian chi tiết (Timeline Engine)
        LocalDate tripDate = request.tripDate();
        LocalTime startTime = request.startTime();
        List<ScheduleItemContext> scheduleItems = timelineEngine.buildTimelineForDay(
                tripDate, startTime, request.originPlace(), orderedPlaces, requestMap, legs);

        // 4. Thẩm định quy tắc xác định bằng RuleEvaluatorService (Gatekeeper)
        ItineraryEvaluationResult evaluationResult = ruleEvaluatorService.evaluateItinerary(scheduleItems);

        double totalDistKm;
        long totalTransitMin;

        if (legs != null && !legs.isEmpty()) {
            double meters = legs.stream().mapToDouble(com.travel.planning.algorithm.model.RouteLeg::getDistanceMeters).sum();
            totalDistKm = Math.round((meters / 1000.0) * 10.0) / 10.0;
            long seconds = legs.stream().mapToLong(com.travel.planning.algorithm.model.RouteLeg::getDurationSeconds).sum();
            totalTransitMin = Math.round(seconds / 60.0);
        } else {
            totalDistKm = scheduleItems.stream()
                    .mapToDouble(ScheduleItemContext::transitDistanceKmFromPrev)
                    .sum();
            totalDistKm = Math.round(totalDistKm * 10.0) / 10.0;

            totalTransitMin = scheduleItems.stream()
                    .mapToLong(ScheduleItemContext::transitMinutesFromPrev)
                    .sum();
        }

        // 5. CỔNG QUYẾT ĐỊNH: SILENT BY DEFAULT NẾU ALL-CLEAR
        if (evaluationResult.isAllClear()) {
            log.info("[TRIP_OPTIMIZER] Itinerary is 100% ALL-CLEAR. AI Copilot remains SILENT (0 token consumed).");
            return new TripOptimizationResponse(
                    "ALL_CLEAR",
                    totalDistKm,
                    totalTransitMin,
                    scheduleItems,
                    evaluationResult,
                    null
            );
        }

        // 6. KÍCH HOẠT AI COPILOT KHI PHÁT HIỆN XUNG ĐỘT
        log.warn("[TRIP_OPTIMIZER] Detected {} issues. Invoking AI Copilot for human-in-the-loop critique.",
                evaluationResult.issues().size());

        Place firstPlaceForWeather = !orderedPlaces.isEmpty() ? orderedPlaces.getFirst() : null;
        String weatherOverview = firstPlaceForWeather != null
                ? fetchWeatherOverview(firstPlaceForWeather, tripDate)
                : "Không có dữ liệu thời tiết.";

        AiCopilotPromptContext copilotContext = new AiCopilotPromptContext(
                request.destination(),
                tripDate,
                scheduleItems,
                evaluationResult.issues(),
                weatherOverview,
                request.userNotes()
        );

        AiProvider aiProvider = aiProviderFactory.getProvider();
        AiCopilotAnalysisResult copilotResult = aiProvider.analyzeAndRecommend(copilotContext);

        return new TripOptimizationResponse(
                "CONFLICTS_DETECTED",
                totalDistKm,
                totalTransitMin,
                scheduleItems,
                evaluationResult,
                copilotResult
        );
    }

    @Override
    public MultiDayOptimizeAndAuditResponse optimizeAndAuditMultiDay(MultiDayOptimizeAndAuditRequest request) {
        log.info("[TRIP_OPTIMIZER] Starting multi-day optimization pipeline for destination='{}', {} days",
                request.destination(), request.days().size());

        List<DayScheduleResponse> dailySchedules = new ArrayList<>();
        List<ScheduleItemContext> allTripItems = new ArrayList<>();
        List<DetectedIssue> allIssues = new ArrayList<>();
        double totalTripDist = 0.0;
        long totalTripTransit = 0;

        // 1. Tối ưu từng ngày độc lập
        for (DayPlanRequest dayPlan : request.days()) {
            OptimizeAndAuditRequest dayReq = new OptimizeAndAuditRequest(
                    request.destination(),
                    dayPlan.dayDate(),
                    dayPlan.startTime(),
                    null,
                    dayPlan.originPlace(),
                    dayPlan.isRoundTrip(),
                    dayPlan.locations(),
                    request.userNotes()
            );

            TripOptimizationResponse dayResponse = optimizeAndAuditItinerary(dayReq);
            allTripItems.addAll(dayResponse.schedule());
            if (dayResponse.evaluation() != null && !dayResponse.evaluation().issues().isEmpty()) {
                allIssues.addAll(dayResponse.evaluation().issues());
            }

            totalTripDist += dayResponse.totalDistanceKm();
            totalTripTransit += dayResponse.totalTransitMinutes();

            dailySchedules.add(new DayScheduleResponse(
                    dayPlan.dayDate(),
                    dayResponse.status(),
                    dayResponse.totalDistanceKm(),
                    dayResponse.totalTransitMinutes(),
                    dayResponse.schedule(),
                    dayResponse.evaluation()
            ));
        }

        // 2. Thẩm định va chạm chồng lấn lịch trình giữa các ngày liên tiếp
        List<DetectedIssue> collisionIssues = timelineCollisionRuleChecker.evaluateCrossDayCollisions(dailySchedules);
        if (!collisionIssues.isEmpty()) {
            allIssues.addAll(collisionIssues);
        }

        boolean allClear = allIssues.stream().noneMatch(i -> i.severity().isCriticalOrImportant());
        String overallStatus = allClear ? "ALL_CLEAR" : "CONFLICTS_DETECTED";

        // 3. Quyết định AI Copilot: Nếu toàn bộ chuyến đi All-Clear -> Im lặng
        if (allClear) {
            log.info("[TRIP_OPTIMIZER] Multi-day trip is ALL-CLEAR. AI Copilot remains SILENT.");
            return new MultiDayOptimizeAndAuditResponse(
                    overallStatus,
                    Math.round(totalTripDist * 10.0) / 10.0,
                    totalTripTransit,
                    dailySchedules,
                    collisionIssues,
                    null
            );
        }

        // 4. Kích hoạt AI Copilot cho toàn bộ chuyến đi khi có xung đột
        log.warn("[TRIP_OPTIMIZER] Multi-day detected {} total issues. Calling AI Copilot.", allIssues.size());
        LocalDate firstDate = !request.days().isEmpty() ? request.days().getFirst().dayDate() : LocalDate.now();

        AiCopilotPromptContext multiDayContext = new AiCopilotPromptContext(
                request.destination(),
                firstDate,
                allTripItems,
                allIssues,
                "Chuyến đi nhiều ngày tại " + request.destination(),
                request.userNotes()
        );

        AiProvider aiProvider = aiProviderFactory.getProvider();
        AiCopilotAnalysisResult copilotResult = aiProvider.analyzeAndRecommend(multiDayContext);

        return new MultiDayOptimizeAndAuditResponse(
                overallStatus,
                Math.round(totalTripDist * 10.0) / 10.0,
                totalTripTransit,
                dailySchedules,
                collisionIssues,
                copilotResult
        );
    }

    @Override
    public TripOptimizationResponse applyCopilotRecommendation(ApplyRecommendationRequest request) {
        log.info("[TRIP_OPTIMIZER] Applying 1-click copilot action: type={}, cardId={}",
                request.actionType(), request.cardId());

        List<ScheduleItemContext> updatedSchedule = new ArrayList<>(request.currentSchedule());
        Map<String, Object> payload = request.actionPayload();

        if (request.actionType() == CopilotActionType.SWAP_PLACES) {
            String targetIdStr = (String) payload.get("targetPlaceId");
            String swapWithIdStr = (String) payload.get("swapWithPlaceId");

            int targetIdx = findIndexById(updatedSchedule, targetIdStr);
            int swapIdx = findIndexById(updatedSchedule, swapWithIdStr);

            if (swapIdx == -1 && targetIdx > 0) {
                swapIdx = targetIdx - 1;
            }

            if (targetIdx != -1 && swapIdx != -1 && targetIdx != swapIdx) {
                Collections.swap(updatedSchedule, targetIdx, swapIdx);
                log.info("[TRIP_OPTIMIZER] Swapped place at index {} with index {}", targetIdx, swapIdx);
            }
        }

        // TÁI TÍNH TOÁN 100% CỰ LY VÀ THỜI GIAN THEO CẶP MỚI
        if (!updatedSchedule.isEmpty()) {
            ScheduleItemContext first = updatedSchedule.getFirst();
            LocalDate dayDate = first.plannedDate() != null ? first.plannedDate() : LocalDate.now();
            LocalTime startTime = first.arrivalTime() != null ? first.arrivalTime().toLocalTime() : LocalTime.of(8, 0);

            updatedSchedule = timelineEngine.recalculateScheduleTimeline(updatedSchedule, null, dayDate, startTime);
        }

        // Tái thẩm định toàn bộ lịch trình
        ItineraryEvaluationResult reEvaluation = ruleEvaluatorService.evaluateItinerary(updatedSchedule);

        double totalDist = updatedSchedule.stream()
                .mapToDouble(ScheduleItemContext::transitDistanceKmFromPrev)
                .sum();
        long totalTransit = updatedSchedule.stream()
                .mapToLong(ScheduleItemContext::transitMinutesFromPrev)
                .sum();

        return new TripOptimizationResponse(
                reEvaluation.isAllClear() ? "ALL_CLEAR" : "UPDATED",
                Math.round(totalDist * 10.0) / 10.0,
                totalTransit,
                updatedSchedule,
                reEvaluation,
                null
        );
    }

    private TspResult solveWithFixedOrigin(OptimizeAndAuditRequest request, List<Place> visitPlaces) {
        TspLocationRequest originReq = request.originPlace();
        UUID originId = originReq.id() != null ? originReq.id() : UUID.randomUUID();

        Place originPlace = Place.builder()
                .id(originId)
                .name(originReq.name())
                .latitude(originReq.latitude())
                .longitude(originReq.longitude())
                .build();

        List<Place> allPlaces = new ArrayList<>();
        allPlaces.add(originPlace);
        allPlaces.addAll(visitPlaces);

        TspRequest tspRequest = TspRequest.builder()
                .places(allPlaces)
                .startPlaceId(originId)
                .roundTrip(request.isRoundTrip())
                .build();

        return heldKarpSolver.solve(tspRequest);
    }

    private List<Place> extractVisitPlaces(List<Place> orderedPlaces, UUID originId) {
        List<Place> visitOrdered = new ArrayList<>();
        for (Place p : orderedPlaces) {
            if (originId == null || !p.getId().equals(originId)) {
                visitOrdered.add(p);
            }
        }
        return visitOrdered;
    }

    private List<Place> solveInProgressReOptimization(
            OptimizeAndAuditRequest request,
            List<Place> visitPlaces,
            Map<UUID, TspLocationRequest> requestMap
    ) {
        List<Place> completedPlaces = new ArrayList<>();
        Place anchorPlace = null;
        List<Place> notVisitedPlaces = new ArrayList<>();

        for (Place p : visitPlaces) {
            TspLocationRequest req = requestMap.get(p.getId());
            VisitStatus status = req != null && req.visitStatus() != null ? req.visitStatus() : VisitStatus.NOT_VISITED;

            if (status == VisitStatus.COMPLETED) {
                completedPlaces.add(p);
            } else if (status == VisitStatus.IN_PROGRESS && anchorPlace == null) {
                anchorPlace = p;
            } else if (status == VisitStatus.NOT_VISITED) {
                notVisitedPlaces.add(p);
            }
            // SKIPPED: bị bỏ qua, không thêm vào lộ trình
        }

        List<Place> result = new ArrayList<>(completedPlaces);

        if (anchorPlace != null) {
            result.add(anchorPlace);
        }

        if (!notVisitedPlaces.isEmpty()) {
            if (anchorPlace != null) {
                // Tối ưu các điểm chưa đi với anchorPlace làm điểm xuất phát
                List<Place> toSolve = new ArrayList<>();
                toSolve.add(anchorPlace);
                toSolve.addAll(notVisitedPlaces);

                TspRequest tspReq = TspRequest.builder()
                        .places(toSolve)
                        .startPlaceId(anchorPlace.getId())
                        .roundTrip(false)
                        .build();

                TspResult tspRes = heldKarpSolver.solve(tspReq);
                for (Place p : tspRes.getOrderedPlaces()) {
                    if (!p.getId().equals(anchorPlace.getId())) {
                        result.add(p);
                    }
                }
            } else {
                TspRequest tspReq = TspRequest.builder()
                        .places(notVisitedPlaces)
                        .roundTrip(request.isRoundTrip())
                        .build();
                TspResult tspRes = heldKarpSolver.solve(tspReq);
                result.addAll(tspRes.getOrderedPlaces());
            }
        }

        return result;
    }

    private String fetchWeatherOverview(Place place, LocalDate tripDate) {
        try {
            WeatherForecastResult forecast = weatherService.getForecast(
                    place.getLatitude(), place.getLongitude(), tripDate, tripDate);

            if (forecast != null && !forecast.dailySummaries().isEmpty()) {
                DailyWeatherSummary summary = forecast.dailySummaries().getFirst();
                return String.format("Nhiệt độ: %.1f°C - %.1f°C, thời tiết chủ đạo: %s, xác suất mưa tối đa: %d%%, lượng mưa: %.1f mm",
                        summary.minTemperature(), summary.maxTemperature(),
                        summary.dominantCondition().getDescription(),
                        summary.maxPrecipitationProbability(), summary.totalRainMm());
            }
        } catch (Exception ex) {
            log.warn("[TRIP_OPTIMIZER] Failed to fetch weather overview: {}", ex.getMessage());
        }
        return "Không có dữ liệu thời tiết cụ thể.";
    }

    private int findIndexById(List<ScheduleItemContext> schedule, String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return -1;
        }
        for (int i = 0; i < schedule.size(); i++) {
            if (schedule.get(i).placeId() != null && schedule.get(i).placeId().toString().equalsIgnoreCase(idStr)) {
                return i;
            }
        }
        return -1;
    }
}
