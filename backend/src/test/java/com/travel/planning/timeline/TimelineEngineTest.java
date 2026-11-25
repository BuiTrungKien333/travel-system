package com.travel.planning.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.travel.place.entity.Place;
import com.travel.place.enums.PlaceType;
import com.travel.planning.dto.request.TspLocationRequest;
import com.travel.planning.rule.model.ScheduleItemContext;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimelineEngineTest {

    private TimelineEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TimelineEngine();
    }

    @Test
    @DisplayName("buildTimelineForDay with originPlace calculates first leg from origin departing at startTime")
    void buildTimeline_withOriginPlace_calculatesFirstLegFromOrigin() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        LocalTime startTime = LocalTime.of(8, 0);

        // Khách sạn xuất phát
        TspLocationRequest origin = new TspLocationRequest(UUID.randomUUID(), "Khách sạn Novotel", 16.0718, 108.2230);

        // Điểm tham quan 1: Cầu Rồng (cách Novotel ~1.5 km, ~3 phút lái xe)
        UUID p1Id = UUID.randomUUID();
        Place p1 = Place.builder()
            .id(p1Id)
            .name("Cầu Rồng")
            .latitude(16.0610)
            .longitude(108.2272)
            .placeType(PlaceType.OUTDOOR)
            .build();

        // Điểm tham quan 2: Bảo tàng Chăm
        UUID p2Id = UUID.randomUUID();
        Place p2 = Place.builder()
            .id(p2Id)
            .name("Bảo tàng Chăm")
            .latitude(16.0592)
            .longitude(108.2240)
            .placeType(PlaceType.INDOOR)
            .build();

        Map<UUID, TspLocationRequest> requestMap = Map.of(
            p1Id,
            new TspLocationRequest(p1Id, "Cầu Rồng", 16.0610, 108.2272, PlaceType.OUTDOOR, null, null, 45),
            p2Id,
            new TspLocationRequest(
                p2Id,
                "Bảo tàng Chăm",
                16.0592,
                108.2240,
                PlaceType.INDOOR,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                60
            )
        );

        List<ScheduleItemContext> timeline = engine.buildTimelineForDay(
            date,
            startTime,
            origin,
            List.of(p1, p2),
            requestMap
        );

        assertThat(timeline).hasSize(2);

        ScheduleItemContext item1 = timeline.get(0);
        assertThat(item1.placeName()).isEqualTo("Cầu Rồng");
        // Giờ đến điểm 1 phải sau 08:00 (đã cộng thời gian di chuyển từ khách sạn)
        assertThat(item1.arrivalTime()).isAfter(date.atTime(startTime));
        assertThat(item1.transitMinutesFromPrev()).isGreaterThan(0);
        assertThat(item1.transitDistanceKmFromPrev()).isGreaterThan(0.0);

        ScheduleItemContext item2 = timeline.get(1);
        assertThat(item2.placeName()).isEqualTo("Bảo tàng Chăm");
        assertThat(item2.arrivalTime()).isEqualTo(item1.departureTime().plusMinutes(item2.transitMinutesFromPrev()));
    }

    @Test
    @DisplayName("buildTimelineForDay correctly flags spillover and midnight crossing")
    void buildTimeline_detectsMidnightAndSpillover() {
        LocalDate date = LocalDate.of(2026, 9, 28);
        LocalTime startTime = LocalTime.of(23, 30); // Xuất phát muộn lúc 23:30

        UUID p1Id = UUID.randomUUID();
        Place p1 = Place.builder()
            .id(p1Id)
            .name("Night Bar")
            .latitude(16.06)
            .longitude(108.22)
            .placeType(PlaceType.INDOOR)
            .build();

        UUID p2Id = UUID.randomUUID();
        Place p2 = Place.builder()
            .id(p2Id)
            .name("Late Night Snack")
            .latitude(16.07)
            .longitude(108.23)
            .placeType(PlaceType.INDOOR)
            .build();

        Map<UUID, TspLocationRequest> requestMap = Map.of(
            p1Id,
            new TspLocationRequest(p1Id, "Night Bar", 16.06, 108.22, PlaceType.INDOOR, null, null, 45), // 23:30 -> 00:15 hôm sau
            p2Id,
            new TspLocationRequest(p2Id, "Late Night Snack", 16.07, 108.23, PlaceType.INDOOR, null, null, 30)
        );

        List<ScheduleItemContext> timeline = engine.buildTimelineForDay(
            date,
            startTime,
            null,
            List.of(p1, p2),
            requestMap
        );

        ScheduleItemContext item1 = timeline.get(0);
        // Đến lúc 23:30 ngày 28/9, rời lúc 00:15 ngày 29/9
        assertThat(item1.displayDate()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(item1.isSpillover()).isFalse();
        assertThat(item1.crossesMidnight()).isTrue();

        ScheduleItemContext item2 = timeline.get(1);
        // Đến lúc sau 00:15 ngày 29/9 -> ngày đến thực tế là 29/9, tràn từ ngày kế hoạch 28/9!
        assertThat(item2.displayDate()).isEqualTo(LocalDate.of(2026, 9, 29));
        assertThat(item2.plannedDate()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(item2.isSpillover()).isTrue();
    }

    @Test
    @DisplayName("recalculateScheduleTimeline recalculates fresh distances and transits between newly adjacent places")
    void recalculateScheduleTimeline_recalculatesLegsAccurately() {
        LocalDate date = LocalDate.of(2026, 10, 1);

        // Vị trí A, B, C theo đường thẳng
        ScheduleItemContext itemA = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm A",
            16.00,
            108.00,
            PlaceType.INDOOR,
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
            date.atTime(8, 0),
            60,
            0.0,
            0
        );
        ScheduleItemContext itemB = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm B",
            16.05,
            108.05,
            PlaceType.INDOOR,
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
            date.atTime(9, 30),
            60,
            50.0,
            60 // Fake old values
        );
        ScheduleItemContext itemC = new ScheduleItemContext(
            UUID.randomUUID(),
            "Điểm C",
            16.10,
            108.10,
            PlaceType.INDOOR,
            LocalTime.of(8, 0),
            LocalTime.of(18, 0),
            date.atTime(11, 0),
            60,
            50.0,
            60
        );

        // Hoán đổi B và C: danh sách mới [A, C, B]
        List<ScheduleItemContext> swapped = List.of(itemA, itemC, itemB);

        List<ScheduleItemContext> recalculated = engine.recalculateScheduleTimeline(
            swapped,
            null,
            date,
            LocalTime.of(8, 0)
        );

        assertThat(recalculated).hasSize(3);
        assertThat(recalculated.get(0).placeName()).isEqualTo("Điểm A");
        assertThat(recalculated.get(1).placeName()).isEqualTo("Điểm C");
        assertThat(recalculated.get(2).placeName()).isEqualTo("Điểm B");

        // Chặng từ A sang C phải có cự ly và thời gian di chuyển được tính mới hoàn toàn
        ScheduleItemContext newC = recalculated.get(1);
        assertThat(newC.transitDistanceKmFromPrev()).isGreaterThan(10.0);
        assertThat(newC.arrivalTime()).isEqualTo(
            recalculated.get(0).departureTime().plusMinutes(newC.transitMinutesFromPrev())
        );
    }
}
