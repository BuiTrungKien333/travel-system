package com.travel.planning.draft.service;

import static com.travel.planning.draft.dto.response.DraftResponse.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.place.entity.Place;
import com.travel.place.enums.PlaceOpeningStatus;
import com.travel.place.model.WeeklyOpeningHours;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.algorithm.model.*;
import com.travel.planning.draft.dto.request.GenerateDraftRequest;
import com.travel.planning.draft.model.DraftTravelMatrix;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.enums.VisitStatus;
import com.travel.planning.retrieval.model.PlaceCandidate;
import com.travel.planning.timeline.TimelineEngine;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adapts the existing solver/engine without changing legacy itinerary endpoints.
 */
@Component
@Profile({ "dev", "rag-demo", "planning-ai" })
public class DraftTimelinePlanner {

    private final HeldKarpSolver solver;
    private final TimelineEngine timeline;
    private final ObjectMapper mapper;

    public DraftTimelinePlanner(HeldKarpSolver solver, TimelineEngine timeline, ObjectMapper mapper) {
        this.solver = solver;
        this.timeline = timeline;
        this.mapper = mapper;
    }

    public Day build(
        int index,
        LocalDate date,
        LocalTime start,
        LocalTime end,
        GenerateDraftRequest.Origin origin,
        boolean roundTrip,
        int restMinutes,
        int bufferMinutes,
        List<PlaceCandidate> selected,
        Map<UUID, Place> entities,
        DraftTravelMatrix travel
    ) {
        var issues = new ArrayList<Problem>();
        var pauses = new ArrayList<Pause>();
        var stops = new ArrayList<Stop>();
        if (selected.isEmpty()) {
            issues.add(new Problem("EMPTY_DAY", false, null, "Ngày này chưa có điểm tham quan được xếp."));
            return new Day(
                index,
                date,
                "",
                "",
                "",
                date.atTime(start),
                date.atTime(start),
                0,
                0,
                List.of(),
                List.of(),
                null,
                issues
            );
        }
        UUID originId = UUID.randomUUID(); // Transient route anchor; never returned as a DB placeId.
        Place anchor = Place.builder()
            .id(originId)
            .name(origin.name())
            .latitude(origin.latitude())
            .longitude(origin.longitude())
            .build();
        List<Place> nodes = new ArrayList<>();
        nodes.add(anchor);
        selected.forEach(p -> nodes.add(entities.get(p.placeId())));
        if (roundTrip) nodes.add(anchor);
        List<Integer> matrixIndices = travel.dayIndices(selected, roundTrip);
        List<Integer> order;
        try {
            order = solver.solveFixedStart(travel.costs(matrixIndices), roundTrip ? nodes.size() - 1 : null);
        } catch (AppException ex) {
            if (ex.getErrorCode() != ErrorCode.RESOURCE_NOT_FOUND) throw ex;
            issues.add(
                new Problem(
                    "ROUTE_UNAVAILABLE",
                    true,
                    null,
                    "Không tìm được tuyến đường bộ nối các điểm đã chọn; hãy đổi điểm xuất phát hoặc điều chỉnh địa điểm."
                )
            );
            return new Day(
                index,
                date,
                "",
                "",
                "",
                date.atTime(start),
                date.atTime(start),
                0,
                0,
                List.of(),
                List.of(),
                null,
                issues
            );
        }
        List<RouteLeg> legs = new ArrayList<>();
        for (int i = 1; i < order.size(); i++) {
            int from = order.get(i - 1),
                to = order.get(i);
            int globalFrom = matrixIndices.get(from),
                globalTo = matrixIndices.get(to);
            legs.add(
                new RouteLeg(
                    nodes.get(from),
                    nodes.get(to),
                    travel.matrix().distances()[globalFrom][globalTo],
                    (long) Math.ceil(travel.matrix().durations()[globalFrom][globalTo])
                )
            );
        }
        Map<UUID, PlaceCandidate> catalog = new HashMap<>();
        selected.forEach(p -> catalog.put(p.placeId(), p));
        LocalDateTime cursor = date.atTime(start);
        int totalTransit = 0;
        double totalDistance = 0;
        Place previous = anchor;
        for (int nodeIndex : order) {
            Place visit = nodes.get(nodeIndex);
            if (visit.getId().equals(originId)) continue;
            if (!stops.isEmpty() && bufferMinutes > 0) {
                pauses.add(new Pause("BUFFER", cursor, cursor.plusMinutes(bufferMinutes)));
                cursor = cursor.plusMinutes(bufferMinutes);
            }
            if (stops.size() == 2 && restMinutes > 0) {
                pauses.add(new Pause("REST", cursor, cursor.plusMinutes(restMinutes)));
                cursor = cursor.plusMinutes(restMinutes);
            }
            PlaceCandidate p = catalog.get(visit.getId());
            WeeklyOpeningHours weekly = null;
            if (visit.getWeeklyOpeningHours() != null) {
                try {
                    weekly = mapper.readValue(visit.getWeeklyOpeningHours(), WeeklyOpeningHours.class);
                } catch (Exception ex) {
                    issues.add(
                        new Problem(
                            "INVALID_WEEKLY_HOURS",
                            false,
                            p.placeId(),
                            "Không đọc được lịch hoạt động theo tuần; cần kiểm tra lại."
                        )
                    );
                }
            }
            var req = new TspLocationRequest(
                p.placeId(),
                visit.getName(),
                visit.getLatitude(),
                visit.getLongitude(),
                visit.getPlaceType(),
                visit.getOpeningTime(),
                visit.getClosingTime(),
                p.estimatedDurationMinutes(),
                VisitStatus.NOT_VISITED,
                weekly
            );
            var from = new TspLocationRequest(
                previous.getId(),
                previous.getName(),
                previous.getLatitude(),
                previous.getLongitude()
            );
            UUID fromId = previous.getId();
            RouteLeg leg = legs
                .stream()
                .filter(l -> l.getFromPlace().getId().equals(fromId) && l.getToPlace().getId().equals(visit.getId()))
                .findFirst()
                .orElseThrow();
            // One-visit calls let this adapter carry forward waiting, rest and buffers accurately.
            var context = timeline
                .buildTimelineForDay(
                    cursor.toLocalDate(),
                    cursor.toLocalTime(),
                    from,
                    List.of(visit),
                    Map.of(p.placeId(), req),
                    List.of(leg)
                )
                .getFirst();
            LocalDateTime visitStart = context.arrivalTime().plusMinutes(context.waitingMinutes());
            LocalDateTime departure = visitStart.plusMinutes(p.estimatedDurationMinutes());
            if (context.openingStatus() == PlaceOpeningStatus.CLOSED) {
                issues.add(new Problem("CLOSED", true, p.placeId(), "Địa điểm đóng cửa trong ngày đã xếp."));
            } else if (
                context.openingStatus() == PlaceOpeningStatus.UNVERIFIED || !"VERIFIED".equals(p.hoursQuality())
            ) {
                issues.add(
                    new Problem("UNVERIFIED_HOURS", false, p.placeId(), "Giờ hoạt động chưa được xác minh đầy đủ.")
                );
            }
            if (
                context.openingStatus() == PlaceOpeningStatus.OPEN &&
                context.closingTime() != null &&
                context.openingTime() != null
            ) {
                LocalDateTime closing = visitStart.toLocalDate().atTime(context.closingTime());
                if (
                    context.openingTime().isAfter(context.closingTime()) &&
                    !visitStart.toLocalTime().isBefore(context.openingTime())
                ) closing = closing.plusDays(1);
                if (departure.isAfter(closing)) issues.add(
                    new Problem("CLOSING_TIME", true, p.placeId(), "Không đủ thời gian tham quan trước giờ đóng cửa.")
                );
            }
            if (!"VERIFIED".equals(p.coordinatesQuality())) issues.add(
                new Problem(
                    "APPROXIMATE_COORDINATES",
                    false,
                    p.placeId(),
                    "Tọa độ địa điểm chưa được xác minh chính xác."
                )
            );
            stops.add(
                new Stop(
                    p,
                    cursor,
                    context.arrivalTime(),
                    visitStart,
                    departure,
                    context.waitingMinutes(),
                    context.transitMinutesFromPrev(),
                    round(leg.getDistanceMeters() / 1000.0),
                    context.openingStatus().name()
                )
            );
            totalTransit += context.transitMinutesFromPrev();
            totalDistance += leg.getDistanceMeters() / 1000.0;
            cursor = departure;
            previous = visit;
        }
        ReturnLeg returnLeg = null;
        if (roundTrip) {
            RouteLeg last = legs.getLast();
            int minutes = (int) Math.ceil(last.getDurationSeconds() / 60.0);
            returnLeg = new ReturnLeg(
                cursor,
                cursor.plusMinutes(minutes),
                minutes,
                round(last.getDistanceMeters() / 1000.0)
            );
            cursor = returnLeg.arrival();
            totalTransit += minutes;
            totalDistance += last.getDistanceMeters() / 1000.0;
        }
        if (cursor.isAfter(date.atTime(end))) issues.add(
            new Problem(
                "DAY_END_EXCEEDED",
                true,
                null,
                "Lịch vượt giờ kết thúc yêu cầu, đã tính cả chặng quay về nếu có."
            )
        );
        return new Day(
            index,
            date,
            "",
            "",
            "",
            date.atTime(start),
            cursor,
            round(totalDistance),
            totalTransit,
            List.copyOf(stops),
            List.copyOf(pauses),
            returnLeg,
            List.copyOf(issues)
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
