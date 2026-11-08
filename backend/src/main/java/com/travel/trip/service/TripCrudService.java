package com.travel.trip.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.util.SecurityUtils;
import com.travel.trip.dto.CreateTripRequest;
import com.travel.trip.dto.UpdateTripDayRequest;
import com.travel.trip.dto.TripDateChangeRequest;
import com.travel.trip.dto.TripResponses.Budget;
import com.travel.trip.dto.TripResponses.Context;
import com.travel.trip.dto.TripResponses.Created;
import com.travel.trip.dto.TripResponses.DateImpact;
import com.travel.trip.dto.TripResponses.Day;
import com.travel.trip.dto.TripResponses.Destination;
import com.travel.trip.dto.TripResponses.Detail;
import com.travel.trip.dto.TripResponses.Page;
import com.travel.trip.dto.TripResponses.Read;
import com.travel.trip.dto.TripResponses.Readiness;
import com.travel.trip.dto.TripResponses.Summary;
import com.travel.trip.dto.UpdateTripRequest;
import com.travel.trip.repository.TripCrudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripCrudService {

    private final TripCrudRepository repository;
    private final ObjectMapper json;
    private final Clock clock;

    @Transactional
    public Created create(CreateTripRequest request) {
        UUID owner = SecurityUtils.getCurrentUserId();
        validateStructure(request);

        String title = request.title().isBlank() ? request.destination().name() : request.title();
        String hash = hash(request, title);

        var existing = repository.byCreateKey(owner, request.clientRequestId());
        if (existing.isPresent()) {
            return replay(existing.get(), hash);
        }

        if (request.startDate().isBefore(TripCalendar.today(clock))) {
            invalid("startDate");
        }
        if (!repository.referencesExist(owner, request)) {
            throw new AppException(ErrorCode.TRIP_INVALID_REFERENCE);
        }

        UUID id = UUID.randomUUID();
        if (!repository.insert(id, owner, request, title, hash, write(request.preferences()))) {
            return replay(repository.byCreateKey(owner, request.clientRequestId()).orElseThrow(), hash);
        }
        repository.initialize(id, request);
        return new Created(read(repository.find(owner, id).orElseThrow(), clock.instant()), true);
    }

    private void validateStructure(CreateTripRequest r) {
        long days = ChronoUnit.DAYS.between(r.startDate(), r.endDate()) + 1;
        if (days < 1 || days > 365) {
            invalid("endDate");
        }
        if (new HashSet<>(r.preferences()).size() != r.preferences().size()) {
            invalid("preferences");
        }
        if (new HashSet<>(r.initialPlaceIds()).size() != r.initialPlaceIds().size()) {
            invalid("initialPlaceIds");
        }
    }

    private void invalid(String field) {
        throw new AppException(ErrorCode.VALIDATION_ERROR, Map.of(field, "Invalid value"));
    }

    private Created replay(TripCrudRepository.Row row, String hash) {
        if (!Objects.equals(row.hash(), hash)) {
            throw new AppException(ErrorCode.TRIP_REQUEST_CONFLICT);
        }
        if (row.deleted()) {
            throw new AppException(ErrorCode.TRIP_NOT_FOUND);
        }
        return new Created(read(row, clock.instant()), false);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Read get(UUID id) {
        return getAt(id, clock.instant());
    }

    /**
     * Reuse the dashboard's evaluation instant, including across a midnight boundary.
     */
    @Transactional(readOnly = true)
    public Read getAt(UUID id, Instant evaluatedAt) {
        return read(repository.find(SecurityUtils.getCurrentUserId(), id)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND)), evaluatedAt);
    }

    @Transactional
    public Read update(UUID id, UpdateTripRequest request) {
        var row = ownedLocked(id);
        requireWritable(row);
        requireVersion(row, request.version());

        if (!request.hasChanges()) {
            invalid("body");
        }
        if (request.has("startDate") != request.has("endDate")) {
            invalid("endDate");
        }
        if (request.has("preferences") && new HashSet<>(request.preferences()).size() != request.preferences().size()) {
            invalid("preferences");
        }
        if (request.has("destination") && !repository.provinceExists(request.destination().provinceId())) {
            throw new AppException(ErrorCode.TRIP_INVALID_REFERENCE);
        }

        var before = new TripCrudRepository.Metadata(
                row.title(), row.destination(), row.provinceId(), row.start(), row.end(),
                row.budget(), row.preferences(), row.accommodation(), row.notes()
        );

        String nextPreferences = request.has("preferences") ? write(request.preferences()) : row.preferences();
        if (request.has("preferences") && preferences(row.preferences()).equals(request.preferences().stream().map(Enum::name).toList())) {
            nextPreferences = row.preferences();
        }

        var next = new TripCrudRepository.Metadata(
                request.has("title") ? request.title() : row.title(),
                request.has("destination") ? request.destination().name() : row.destination(),
                request.has("destination") ? request.destination().provinceId() : row.provinceId(),
                request.has("startDate") ? request.startDate() : row.start(),
                request.has("endDate") ? request.endDate() : row.end(),
                request.has("budget") ? (request.budget() == null ? null : request.budget().amount().longValueExact()) : row.budget(),
                nextPreferences,
                request.has("accommodation") ? request.accommodation() : row.accommodation(),
                request.has("notes") ? request.notes() : row.notes()
        );

        if (next.start() != null || next.end() != null) {
            validateRange(next.start(), next.end());
        }
        if (before.equals(next)) {
            return read(row, clock.instant());
        }

        boolean datesChanged = !Objects.equals(row.start(), next.start()) || !Objects.equals(row.end(), next.end());
        Instant now = clock.instant();
        if (datesChanged) {
            var impact = impact(row, next.start(), next.end());
            if (impact.requiresAcknowledgement() && !request.acknowledgeDateImpact()) {
                throw new AppException(ErrorCode.TRIP_DATE_IMPACT);
            }
            repository.changeDates(id, next.start(), next.end(), now);
        }

        if (!repository.update(id, row.version(), next, now)) {
            throw new AppException(ErrorCode.TRIP_VERSION_CONFLICT);
        }
        return read(repository.find(SecurityUtils.getCurrentUserId(), id).orElseThrow(), now);
    }
    @Transactional
    public Read updateDay(UUID id, LocalDate date, UpdateTripDayRequest request) {
        var row = ownedLocked(id);
        requireWritable(row);
        requireVersion(row, request.getVersion());
        if (row.start() == null || row.end() == null || date.isBefore(row.start()) || date.isAfter(row.end())) invalid("date");
        var day = repository.days(id, row.start()).stream().filter(value -> value.date().equals(date)).findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.VALIDATION_ERROR, Map.of("date", "Day is not in this trip")));
        if (request.getReturnToStart() && request.getStartLocation() == null) invalid("startLocation");
        if (!request.hasStartCoordinate()) request.setStartCoordinate(
                Objects.equals(day.startLocation(), request.getStartLocation()) ? day.startCoordinate() : null);
        if (request.getStartCoordinate() != null && request.getStartLocation() == null) invalid("startLocation");
        var schedule = repository.schedule(id);
        boolean changedDuration = false;
        if (!request.getDurations().isEmpty()) {
            if (request.getExpectedItineraryUpdatedAt() == null) invalid("expectedItineraryUpdatedAt");
            if (schedule.isEmpty() || !schedule.get().updatedAt().equals(request.getExpectedItineraryUpdatedAt()))
                throw new AppException(ErrorCode.TRIP_VERSION_CONFLICT);
            var durations = repository.durations(schedule.get().id(), date);
            Set<UUID> seen = new HashSet<>();
            for (var value : request.getDurations()) {
                if (!seen.add(value.stopId())) invalid("durations");
                if (!durations.containsKey(value.stopId())) throw new AppException(ErrorCode.TRIP_VERSION_CONFLICT);
                changedDuration |= !value.durationMinutes().equals(durations.get(value.stopId()));
            }
        }
        boolean changed = !Objects.equals(day.startLocation(), request.getStartLocation())
                || !Objects.equals(day.startCoordinate(), request.getStartCoordinate())
                || !Objects.equals(day.startTime(), request.getStartTime())
                || day.returnToStart() != request.getReturnToStart()
                || !day.transport().equals(request.getTransport().name());
        if (!changed && !changedDuration) return read(row, clock.instant());
        Instant now = clock.instant();
        // A day-setting change must also invalidate optimization proposals based on the previous schedule.
        if (schedule.isPresent() && !now.isAfter(schedule.get().updatedAt())) now = schedule.get().updatedAt().plusNanos(1000);
        repository.updateDay(id, date, request, now);
        if (schedule.isPresent()) {
            for (var value : request.getDurations()) repository.updateDuration(schedule.get().id(), date, value, now);
            repository.touchSchedule(schedule.get().id(), now);
        }
        repository.touchTrip(id, now);
        return read(repository.find(SecurityUtils.getCurrentUserId(), id).orElseThrow(), now);
    }
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public DateImpact previewDates(UUID id, TripDateChangeRequest request) {
        var row = repository.find(SecurityUtils.getCurrentUserId(), id)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
        requireWritable(row);
        requireVersion(row, request.version());
        validateRange(request.startDate(), request.endDate());
        return impact(row, request.startDate(), request.endDate());
    }

    @Transactional
    public Read complete(UUID id, long version) {
        if (version < 0) {
            invalid("version");
        }
        var row = ownedLocked(id);
        requireWritable(row);
        Instant now = clock.instant();

        // A lost response may be retried with the version from before completion.
        if ("COMPLETED".equals(row.status())) {
            return read(row, now);
        }
        requireVersion(row, version);
        if (!"LIVE".equals(TripCalendar.mode(row.status(), row.start(), row.end(), context(now).evaluatedDate()))) {
            throw new AppException(ErrorCode.TRIP_READ_ONLY);
        }
        if (!repository.complete(id, version, now)) {
            throw new AppException(ErrorCode.TRIP_VERSION_CONFLICT);
        }
        return read(repository.find(SecurityUtils.getCurrentUserId(), id).orElseThrow(), now);
    }

    @Transactional
    public void delete(UUID id, long version) {
        if (version < 0) {
            invalid("version");
        }
        var row = ownedLocked(id);
        if (row.deleted()) {
            return;
        }
        requireVersion(row, version);
        repository.softDelete(id, clock.instant());
    }

    private TripCrudRepository.Row ownedLocked(UUID id) {
        return repository.lockOwned(SecurityUtils.getCurrentUserId(), id)
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
    }

    private void requireWritable(TripCrudRepository.Row row) {
        if (row.deleted()) {
            throw new AppException(ErrorCode.TRIP_NOT_FOUND);
        }
        if ("CANCELLED".equals(row.status())) {
            throw new AppException(ErrorCode.TRIP_READ_ONLY);
        }
    }

    private void requireVersion(TripCrudRepository.Row row, long version) {
        if (row.version() != version) {
            throw new AppException(ErrorCode.TRIP_VERSION_CONFLICT);
        }
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || TripCalendar.days(start, end) < 1 || ChronoUnit.DAYS.between(start, end) >= 365) {
            invalid("endDate");
        }
    }

    private DateImpact impact(TripCrudRepository.Row row, LocalDate start, LocalDate end) {
        var existing = repository.days(row.id(), row.start()).stream().map(Day::date).toList();
        var desired = start.datesUntil(end.plusDays(1)).toList();
        var kept = new HashSet<>(desired);
        var previous = new HashSet<>(existing);
        long moved = repository.movedToPoolCount(row.id(), start, end);
        return new DateImpact(
                row.version(),
                start,
                end,
                desired.size(),
                existing.stream().filter(date -> !kept.contains(date)).toList(),
                desired.stream().filter(date -> !previous.contains(date)).toList(),
                moved,
                moved > 0
        );
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<Summary> list(int page, int size, String mode, String q, String sort) {
        if (page < 0 || size < 1 || size > 100) {
            invalid("page");
        }
        if (q != null && q.trim().length() > 120) {
            invalid("q");
        }
        if (mode != null && !Set.of("PLANNING", "LIVE", "RECAP", "CANCELLED").contains(mode)) {
            invalid("mode");
        }

        String[] parts = sort.split(",", -1);
        Map<String, String> columns = Map.of(
                "createdAt", "t.created_at",
                "updatedAt", "t.updated_at",
                "startDate", "t.start_date"
        );
        if (parts.length != 2 || !columns.containsKey(parts[0]) || !Set.of("asc", "desc").contains(parts[1])) {
            invalid("sort");
        }

        String order = columns.get(parts[0]) + " " + parts[1] + " NULLS LAST,t.id " + parts[1];
        Instant now = clock.instant();
        Context context = context(now);
        var filter = repository.filter(SecurityUtils.getCurrentUserId(), mode, q, context.evaluatedDate());
        long total = repository.count(filter);
        var rows = repository.list(filter, order, size, (long) page * size);
        return new Page<>(
                rows.stream().map(r -> summary(r, context.evaluatedDate())).toList(),
                page,
                size,
                total,
                (total + size - 1) / size,
                context
        );
    }

    private Context context(Instant now) {
        return new Context(now, now.atZone(TripCalendar.ZONE).toLocalDate());
    }

    private Summary summary(TripCrudRepository.Row r, LocalDate today) {
        int count = TripCalendar.days(r.start(), r.end());
        return new Summary(
                r.id(),
                r.title(),
                new Destination(r.destination(), r.provinceId()),
                r.start(),
                r.end(),
                r.zone(),
                r.status(),
                TripCalendar.mode(r.status(), r.start(), r.end(), today),
                new Readiness(count > 0, r.stopCount() > 0, r.unscheduledCount()),
                count,
                r.stopCount(),
                r.memoryCount(),
                r.coverImage(),
                r.version(),
                r.created(),
                r.updated()
        );
    }

    private Read read(TripCrudRepository.Row r, Instant now) {
        Context context = context(now);
        Summary s = summary(r, context.evaluatedDate());
        Detail detail = new Detail(
                s.id(),
                s.title(),
                s.destination(),
                s.startDate(),
                s.endDate(),
                s.timeZone(),
                s.status(),
                s.mode(),
                s.readiness(),
                s.dayCount(),
                s.stopCount(),
                s.memoryCount(),
                s.coverImageUrl(),
                s.version(),
                s.createdAt(),
                s.updatedAt(),
                r.budget() == null ? null : new Budget(r.budget(), "VND"),
                preferences(r.preferences()),
                r.accommodation(),
                r.notes(),
                repository.days(r.id(), r.start())
        );
        return new Read(detail, context);
    }

    private List<String> preferences(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(value, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Legacy trip preferences require migration", e);
        }
    }

    private String hash(CreateTripRequest r, String title) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("title", title);
        normalized.put("destination", r.destination());
        normalized.put("startDate", r.startDate());
        normalized.put("endDate", r.endDate());
        normalized.put("timeZone", r.timeZone());
        normalized.put("budget", r.budget() == null ? null : r.budget().amount().longValueExact());
        normalized.put("preferences", r.preferences().stream().map(Enum::name).sorted().toList());
        normalized.put("accommodation", r.accommodation());
        normalized.put("notes", r.notes());
        normalized.put("initialPlaceIds", r.initialPlaceIds().stream().map(UUID::toString).sorted().toList());

        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(write(normalized).getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
