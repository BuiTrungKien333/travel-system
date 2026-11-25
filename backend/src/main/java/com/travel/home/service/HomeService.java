package com.travel.home.service;

import com.travel.home.dto.HomeDashboard;
import com.travel.home.repository.HomeRepository;
import com.travel.security.util.SecurityUtils;
import com.travel.trip.dto.TripResponses;
import com.travel.trip.service.TripCalendar;
import com.travel.trip.service.TripCrudService;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final HomeRepository repository;
    private final TripCrudService trips;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public HomeDashboard get() {
        var owner = SecurityUtils.getCurrentUserId();
        var now = clock.instant();
        var today = TripCalendar.today(Clock.fixed(now, TripCalendar.ZONE));
        var selected = repository
            .selectedTrip(owner, today)
            .map(id -> trips.getAt(id, now))
            .orElse(null);

        String state = "NEW";
        Integer dayIndex = null;

        if (selected != null) {
            var trip = selected.trip();
            state = switch (trip.mode()) {
                case "LIVE" -> "LIVE";
                case "RECAP" -> "RETURNING";
                default -> trip.startDate() != null &&
                trip.startDate().isAfter(today) &&
                !trip.startDate().isAfter(today.plusDays(7))
                    ? "UPCOMING"
                    : "PLANNING";
            };
            if (state.equals("LIVE")) {
                dayIndex = Math.toIntExact(ChronoUnit.DAYS.between(trip.startDate(), today));
            }
        }

        var id = selected == null ? null : selected.trip().id();
        boolean live = state.equals("LIVE");

        return new HomeDashboard(
            state,
            selected,
            dayIndex,
            live ? repository.todayCount(id, today) : 0,
            live ? repository.nextStop(id, today) : null,
            state.equals("PLANNING") || state.equals("UPCOMING") ? repository.firstEmptyDay(id) : null,
            repository.footprint(owner, today),
            repository.photos(owner, live ? id : null, today),
            new TripResponses.Context(now, today)
        );
    }
}
