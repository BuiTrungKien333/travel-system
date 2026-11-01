package com.travel.planning.draft;

import com.travel.planning.ai.intent.TravelIntentService;
import com.travel.planning.retrieval.PlaceCandidate;
import com.travel.planning.retrieval.PlaceRetrievalService;

import java.time.*;
import java.util.*;

public final class DraftModels {
    private DraftModels() {
    }

    public record Problem(String code, boolean blocking, UUID placeId, String message) {
    }

    public record Pause(String type, LocalDateTime start, LocalDateTime end) {
    }

    public record Stop(PlaceCandidate place, LocalDateTime transitDeparture, LocalDateTime arrival,
                       LocalDateTime visitStart, LocalDateTime departure, int waitingMinutes, int transitMinutes,
                       double distanceKm, String openingStatus) {
    }

    public record ReturnLeg(LocalDateTime departure, LocalDateTime arrival, int minutes, double distanceKm) {
    }

    public record Day(int dayIndex, LocalDate date, String title, String summary, String summarySource,
                      LocalDateTime start, LocalDateTime finish, double totalDistanceKm, int totalTransitMinutes,
                      List<Stop> stops, List<Pause> pauses, ReturnLeg returnToOrigin, List<Problem> issues) {
        public boolean blocked() {
            return issues.stream().anyMatch(Problem::blocking);
        }

        public Day narrated(String title, String summary, String source) {
            return new Day(dayIndex, date, title, summary, source, start, finish, totalDistanceKm,
                    totalTransitMinutes, stops, pauses, returnToOrigin, issues);
        }
    }

    public record Unassigned(PlaceCandidate place, String reason) {
    }

    public record WeatherAdvice(String status, String provider, String coverage, List<WeatherDay> days) {
    }

    public record WeatherDay(LocalDate date, Double minTemperature, Double maxTemperature, Integer rainProbability,
                             String condition, String riskLevel) {
    }

    public record Response(String status, LocalDate startDate, String timezone,
                           TravelIntentService.IntentAnalysis analysis, PlaceRetrievalService.SearchResult candidates,
                           List<Day> days, List<Unassigned> unassignedPlaces, List<Problem> issues,
                           WeatherAdvice weather, Map<String, String> metadata) {
    }
}
