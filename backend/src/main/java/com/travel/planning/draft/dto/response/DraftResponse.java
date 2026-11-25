package com.travel.planning.draft.dto.response;

import com.travel.planning.ai.intent.dto.response.IntentAnalysisResponse;
import com.travel.planning.ai.resilience.AiRequestContext;
import com.travel.planning.retrieval.dto.response.PlaceSearchResponse;
import com.travel.planning.retrieval.model.PlaceCandidate;
import java.time.*;
import java.util.*;

public record DraftResponse(
    String status,
    LocalDate startDate,
    String timezone,
    IntentAnalysisResponse analysis,
    PlaceSearchResponse candidates,
    List<Day> days,
    List<Unassigned> unassignedPlaces,
    List<Problem> issues,
    WeatherAdvice weather,
    Map<String, String> metadata,
    AiRequestContext.Report executionReport
) {
    public record Problem(String code, boolean blocking, UUID placeId, String message) {}

    public record Pause(String type, LocalDateTime start, LocalDateTime end) {}

    public record Stop(
        PlaceCandidate place,
        LocalDateTime transitDeparture,
        LocalDateTime arrival,
        LocalDateTime visitStart,
        LocalDateTime departure,
        int waitingMinutes,
        int transitMinutes,
        double distanceKm,
        String openingStatus
    ) {}

    public record ReturnLeg(LocalDateTime departure, LocalDateTime arrival, int minutes, double distanceKm) {}

    public record Day(
        int dayIndex,
        LocalDate date,
        String title,
        String summary,
        String summarySource,
        LocalDateTime start,
        LocalDateTime finish,
        double totalDistanceKm,
        int totalTransitMinutes,
        List<Stop> stops,
        List<Pause> pauses,
        ReturnLeg returnToOrigin,
        List<Problem> issues
    ) {
        public boolean blocked() {
            return issues.stream().anyMatch(Problem::blocking);
        }

        public Day narrated(String title, String summary, String source) {
            return new Day(
                dayIndex,
                date,
                title,
                summary,
                source,
                start,
                finish,
                totalDistanceKm,
                totalTransitMinutes,
                stops,
                pauses,
                returnToOrigin,
                issues
            );
        }
    }

    public record Unassigned(PlaceCandidate place, String reason) {}

    public record WeatherAdvice(String status, String provider, String coverage, List<WeatherDay> days) {}

    public record WeatherDay(
        LocalDate date,
        Double minTemperature,
        Double maxTemperature,
        Integer rainProbability,
        String condition,
        String riskLevel
    ) {}
}
