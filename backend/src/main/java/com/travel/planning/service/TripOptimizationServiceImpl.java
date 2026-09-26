package com.travel.planning.service;

import com.travel.place.entity.Place;
import com.travel.place.enums.PlaceType;
import com.travel.place.repository.PlaceRepository;
import com.travel.planning.ai.copilot.enums.CopilotActionType;
import com.travel.planning.ai.copilot.model.AiCopilotAnalysisResult;
import com.travel.planning.ai.copilot.model.AiCopilotPromptContext;
import com.travel.planning.ai.provider.AiProvider;
import com.travel.planning.ai.provider.AiProviderFactory;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.algorithm.model.RouteLeg;
import com.travel.planning.algorithm.model.TspRequest;
import com.travel.planning.algorithm.model.TspResult;
import com.travel.planning.dto.request.ApplyRecommendationRequest;
import com.travel.planning.dto.request.OptimizeAndAuditRequest;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.dto.response.TripOptimizationResponse;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.planning.rule.service.RuleEvaluatorService;
import com.travel.weather.model.DailyWeatherSummary;
import com.travel.weather.model.WeatherForecastResult;
import com.travel.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "TRIP_OPTIMIZATION_SERVICE")
public class TripOptimizationServiceImpl implements TripOptimizationService {

    private final HeldKarpSolver heldKarpSolver;
    private final WeatherService weatherService;
    private final RuleEvaluatorService ruleEvaluatorService;
    private final AiProviderFactory aiProviderFactory;
    private final PlaceRepository placeRepository;

    @Override
    public TripOptimizationResponse optimizeAndAuditItinerary(OptimizeAndAuditRequest request) {
        log.info("[TRIP_OPTIMIZER] Starting full optimization and audit pipeline for destination='{}', {} locations, date={}",
                request.destination(), request.locations().size(), request.tripDate());

        // 1. Chuyển đổi DTO sang List<Place> và truy vấn bổ sung thông tin từ DB (nếu có)
        Map<UUID, TspLocationRequest> requestMap = new HashMap<>();
        List<Place> places = new ArrayList<>();

        for (TspLocationRequest loc : request.locations()) {
            UUID placeId = loc.id() != null ? loc.id() : UUID.randomUUID();
            requestMap.put(placeId, loc);

            PlaceType placeType = loc.placeType() != null ? loc.placeType() : PlaceType.UNSPECIFIED;
            LocalTime open = loc.openingTime();
            LocalTime close = loc.closingTime();

            // Nếu chưa có giờ mở/đóng cửa hoặc loại không gian, tra cứu từ CSDL Place
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
                }
            }

            places.add(Place.builder()
                    .id(placeId)
                    .name(loc.name())
                    .latitude(loc.latitude())
                    .longitude(loc.longitude())
                    .placeType(placeType)
                    .openingTime(open)
                    .closingTime(close)
                    .build());
        }

        // 2. Chạy thuật toán Held-Karp giải quyết bài toán TSP
        TspRequest tspRequest = TspRequest.builder()
                .places(places)
                .startPlaceId(request.startPlaceId())
                .roundTrip(request.isRoundTrip())
                .build();

        TspResult tspResult = heldKarpSolver.solve(tspRequest);
        List<Place> orderedPlaces = tspResult.getOrderedPlaces();

        // 3. Xây dựng trục thời gian chi tiết (Timeline Schedule)
        LocalDate tripDate = request.tripDate();
        LocalTime startTime = request.startTime();
        List<ScheduleItemContext> scheduleItems = buildTimeline(orderedPlaces, tspResult.getLegs(), requestMap, tripDate, startTime);

        // 4. Thẩm định các quy tắc xác định bằng RuleEvaluatorService (Gatekeeper)
        ItineraryEvaluationResult evaluationResult = ruleEvaluatorService.evaluateItinerary(scheduleItems);

        double totalDistKm = Math.round((tspResult.getTotalDistanceMeters() / 1000.0) * 10.0) / 10.0;
        long totalTransitMin = Math.round(tspResult.getTotalDurationSeconds() / 60.0);

        // 5. CỔNG QUYẾT ĐỊNH: SILENT BY DEFAULT
        if (evaluationResult.isAllClear()) {
            log.info("[TRIP_OPTIMIZER] Itinerary is 100% ALL-CLEAR. AI Copilot remains SILENT (0 token consumed).");
            return new TripOptimizationResponse(
                    "ALL_CLEAR",
                    totalDistKm,
                    totalTransitMin,
                    scheduleItems,
                    evaluationResult,
                    null // Không kích hoạt AI
            );
        }

        // 6. KÍCH HOẠT AI COPILOT KHI PHÁT HIỆN XUNG ĐỘT
        log.warn("[TRIP_OPTIMIZER] Detected {} issues. Invoking AI Copilot for human-in-the-loop critique.",
                evaluationResult.issues().size());

        String weatherOverview = fetchWeatherOverview(orderedPlaces.getFirst(), tripDate);
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

            // Nếu không truyền swapWithPlaceId cụ thể, tự động hoán đổi với điểm liền trước để đến sớm hơn
            if (swapIdx == -1 && targetIdx > 0) {
                swapIdx = targetIdx - 1;
            }

            if (targetIdx != -1 && swapIdx != -1 && targetIdx != swapIdx) {
                Collections.swap(updatedSchedule, targetIdx, swapIdx);
                log.info("[TRIP_OPTIMIZER] Swapped place at index {} with index {}", targetIdx, swapIdx);
            }
        } else if (request.actionType() == CopilotActionType.CHANGE_TIME) {
            Number bufferAdd = (Number) payload.get("bufferAddMinutes");
            int addMin = bufferAdd != null ? bufferAdd.intValue() : 15;
            log.info("[TRIP_OPTIMIZER] Adjusting transit buffer: +{} minutes", addMin);
        }

        // Tái tính toán lại trục thời gian sau khi hoán đổi
        if (!updatedSchedule.isEmpty()) {
            ScheduleItemContext first = updatedSchedule.getFirst();
            LocalDateTime time = first.arrivalTime();
            List<ScheduleItemContext> recalculated = new ArrayList<>();

            for (int i = 0; i < updatedSchedule.size(); i++) {
                ScheduleItemContext item = updatedSchedule.get(i);
                int duration = item.durationMinutes();
                double dist = item.transitDistanceKmFromPrev();
                int transit = item.transitMinutesFromPrev();

                LocalDateTime arrival = (i == 0) ? time : time.plusMinutes(transit);
                LocalDateTime departure = arrival.plusMinutes(duration);
                time = departure;

                recalculated.add(new ScheduleItemContext(
                        item.placeId(),
                        item.placeName(),
                        item.latitude(),
                        item.longitude(),
                        item.placeType(),
                        item.openingTime(),
                        item.closingTime(),
                        arrival,
                        duration,
                        dist,
                        transit
                ));
            }
            updatedSchedule = recalculated;
        }

        // Tái thẩm định lại toàn bộ lịch trình
        ItineraryEvaluationResult reEvaluation = ruleEvaluatorService.evaluateItinerary(updatedSchedule);

        double totalDist = updatedSchedule.stream().mapToDouble(ScheduleItemContext::transitDistanceKmFromPrev).sum();
        long totalTransit = updatedSchedule.stream().mapToLong(ScheduleItemContext::transitMinutesFromPrev).sum();

        return new TripOptimizationResponse(
                reEvaluation.isAllClear() ? "ALL_CLEAR" : "UPDATED",
                Math.round(totalDist * 10.0) / 10.0,
                totalTransit,
                updatedSchedule,
                reEvaluation,
                null
        );
    }

    private List<ScheduleItemContext> buildTimeline(
            List<Place> orderedPlaces,
            List<RouteLeg> legs,
            Map<UUID, TspLocationRequest> requestMap,
            LocalDate tripDate,
            LocalTime startTime
    ) {
        List<ScheduleItemContext> scheduleItems = new ArrayList<>();
        LocalDateTime currentTime = tripDate.atTime(startTime);

        for (int i = 0; i < orderedPlaces.size(); i++) {
            Place p = orderedPlaces.get(i);
            TspLocationRequest locReq = requestMap.get(p.getId());

            int duration = (locReq != null && locReq.durationMinutes() != null && locReq.durationMinutes() > 0)
                    ? locReq.durationMinutes()
                    : 60;

            double distFromPrev = 0.0;
            int transitFromPrev = 0;

            if (i > 0 && legs != null && (i - 1) < legs.size()) {
                RouteLeg leg = legs.get(i - 1);
                distFromPrev = Math.round((leg.getDistanceMeters() / 1000.0) * 10.0) / 10.0;
                transitFromPrev = (int) Math.ceil(leg.getDurationSeconds() / 60.0);
            }

            LocalDateTime arrivalTime = (i == 0) ? currentTime : currentTime.plusMinutes(transitFromPrev);
            LocalDateTime departureTime = arrivalTime.plusMinutes(duration);
            currentTime = departureTime;

            scheduleItems.add(new ScheduleItemContext(
                    p.getId(),
                    p.getName(),
                    p.getLatitude(),
                    p.getLongitude(),
                    p.getPlaceType() != null ? p.getPlaceType() : PlaceType.UNSPECIFIED,
                    p.getOpeningTime(),
                    p.getClosingTime(),
                    arrivalTime,
                    duration,
                    distFromPrev,
                    transitFromPrev
            ));
        }

        return scheduleItems;
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
