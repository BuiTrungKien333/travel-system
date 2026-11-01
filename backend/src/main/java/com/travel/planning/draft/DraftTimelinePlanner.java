package com.travel.planning.draft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.place.entity.Place;
import com.travel.place.enums.PlaceOpeningStatus;
import com.travel.place.model.WeeklyOpeningHours;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.algorithm.model.*;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.enums.VisitStatus;
import com.travel.planning.retrieval.PlaceCandidate;
import com.travel.planning.timeline.TimelineEngine;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

import static com.travel.planning.draft.DraftModels.*;

/** Adapts the existing solver/engine without changing legacy itinerary endpoints. */
@Component
@Profile("rag-demo")
public class DraftTimelinePlanner {
    private final HeldKarpSolver solver;
    private final TimelineEngine timeline;
    private final ObjectMapper mapper;
    public DraftTimelinePlanner(HeldKarpSolver solver, TimelineEngine timeline, ObjectMapper mapper) {
        this.solver = solver; this.timeline = timeline; this.mapper = mapper;
    }

    public Day build(int index, LocalDate date, LocalTime start, LocalTime end, DraftController.Origin origin,
                     boolean roundTrip, int restMinutes, int bufferMinutes,
                     List<PlaceCandidate> selected, Map<UUID, Place> entities) {
        var issues = new ArrayList<Problem>(); var pauses = new ArrayList<Pause>(); var stops = new ArrayList<Stop>();
        if (selected.isEmpty()) {
            issues.add(new Problem("EMPTY_DAY", false, null, "Ngày này chưa có điểm tham quan được xếp."));
            return new Day(index, date, "", "", "", date.atTime(start), date.atTime(start), 0, 0, List.of(), List.of(), null, issues);
        }
        UUID originId = UUID.randomUUID(); // Transient route anchor; never returned as a DB placeId.
        Place anchor = Place.builder().id(originId).name(origin.name()).latitude(origin.latitude()).longitude(origin.longitude()).build();
        List<Place> nodes = new ArrayList<>(); nodes.add(anchor);
        selected.forEach(p -> nodes.add(entities.get(p.placeId())));
        var tsp = solver.solve(TspRequest.builder().places(nodes).startPlaceId(originId).roundTrip(roundTrip).build());
        Map<UUID, PlaceCandidate> catalog = new HashMap<>(); selected.forEach(p -> catalog.put(p.placeId(), p));
        LocalDateTime cursor = date.atTime(start); int totalTransit = 0; double totalDistance = 0;
        Place previous = anchor;
        for (Place visit : tsp.getOrderedPlaces()) {
            if (visit.getId().equals(originId)) continue;
            if (!stops.isEmpty() && bufferMinutes > 0) {
                pauses.add(new Pause("BUFFER", cursor, cursor.plusMinutes(bufferMinutes))); cursor = cursor.plusMinutes(bufferMinutes);
            }
            if (stops.size() == 2 && restMinutes > 0) {
                pauses.add(new Pause("REST", cursor, cursor.plusMinutes(restMinutes))); cursor = cursor.plusMinutes(restMinutes);
            }
            PlaceCandidate p = catalog.get(visit.getId());
            WeeklyOpeningHours weekly = null;
            if (visit.getWeeklyOpeningHours() != null) {
                try { weekly = mapper.readValue(visit.getWeeklyOpeningHours(), WeeklyOpeningHours.class); }
                catch (Exception ex) { issues.add(new Problem("INVALID_WEEKLY_HOURS", false, p.placeId(), "Không đọc được lịch hoạt động theo tuần; cần kiểm tra lại.")); }
            }
            var req = new TspLocationRequest(p.placeId(), visit.getName(), visit.getLatitude(), visit.getLongitude(),
                    visit.getPlaceType(), visit.getOpeningTime(), visit.getClosingTime(), p.estimatedDurationMinutes(), VisitStatus.NOT_VISITED, weekly);
            var from = new TspLocationRequest(previous.getId(), previous.getName(), previous.getLatitude(), previous.getLongitude());
            UUID fromId = previous.getId();
            RouteLeg leg = tsp.getLegs().stream().filter(l -> l.getFromPlace().getId().equals(fromId)
                    && l.getToPlace().getId().equals(visit.getId())).findFirst().orElseThrow();
            // One-visit calls let this adapter carry forward waiting, rest and buffers accurately.
            var context = timeline.buildTimelineForDay(cursor.toLocalDate(), cursor.toLocalTime(), from,
                    List.of(visit), Map.of(p.placeId(), req), List.of(leg)).getFirst();
            LocalDateTime visitStart = context.arrivalTime().plusMinutes(context.waitingMinutes());
            LocalDateTime departure = visitStart.plusMinutes(p.estimatedDurationMinutes());
            if (context.openingStatus() == PlaceOpeningStatus.CLOSED) {
                issues.add(new Problem("CLOSED", true, p.placeId(), "Địa điểm đóng cửa trong ngày đã xếp."));
            } else if (context.openingStatus() == PlaceOpeningStatus.UNVERIFIED || !"VERIFIED".equals(p.hoursQuality())) {
                issues.add(new Problem("UNVERIFIED_HOURS", false, p.placeId(), "Giờ hoạt động chưa được xác minh đầy đủ."));
            }
            if (context.openingStatus() == PlaceOpeningStatus.OPEN && context.closingTime() != null && context.openingTime() != null) {
                LocalDateTime closing = visitStart.toLocalDate().atTime(context.closingTime());
                if (context.openingTime().isAfter(context.closingTime()) && !visitStart.toLocalTime().isBefore(context.openingTime())) closing = closing.plusDays(1);
                if (departure.isAfter(closing)) issues.add(new Problem("CLOSING_TIME", true, p.placeId(), "Không đủ thời gian tham quan trước giờ đóng cửa."));
            }
            if (!"VERIFIED".equals(p.coordinatesQuality())) issues.add(new Problem("APPROXIMATE_COORDINATES", false, p.placeId(), "Tọa độ điểm dừng chỉ là xấp xỉ demo."));
            stops.add(new Stop(p, cursor, context.arrivalTime(), visitStart, departure, context.waitingMinutes(),
                    context.transitMinutesFromPrev(), context.transitDistanceKmFromPrev(), context.openingStatus().name()));
            totalTransit += context.transitMinutesFromPrev(); totalDistance += leg.getDistanceMeters() / 1000.0;
            cursor = departure; previous = visit;
        }
        ReturnLeg returnLeg = null;
        if (roundTrip) {
            RouteLeg last = tsp.getLegs().getLast();
            int minutes = (int) Math.ceil(last.getDurationSeconds() / 60.0);
            returnLeg = new ReturnLeg(cursor, cursor.plusMinutes(minutes), minutes, round(last.getDistanceMeters() / 1000.0));
            cursor = returnLeg.arrival(); totalTransit += minutes; totalDistance += last.getDistanceMeters() / 1000.0;
        }
        if (cursor.isAfter(date.atTime(end))) issues.add(new Problem("DAY_END_EXCEEDED", true, null, "Lịch vượt giờ kết thúc yêu cầu, đã tính cả chặng quay về nếu có."));
        return new Day(index, date, "", "", "", date.atTime(start), cursor, round(totalDistance), totalTransit,
                List.copyOf(stops), List.copyOf(pauses), returnLeg, List.copyOf(issues));
    }

    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
