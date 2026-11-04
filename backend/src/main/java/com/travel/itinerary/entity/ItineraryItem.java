package com.travel.itinerary.entity;

import com.travel.place.entity.Place;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ItineraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "arrival_at")
    private LocalDateTime arrivalAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "distance_from_previous")
    private Double distanceFromPrevious;

    @Column(name = "travel_time_from_previous")
    private Integer travelTimeFromPrevious;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void setSchedule(LocalDate plannedDate, LocalDateTime arrivalAt, int durationMinutes) {
        this.date = plannedDate;
        this.arrivalAt = arrivalAt;
        this.durationMinutes = durationMinutes;
        this.startTime = arrivalAt == null ? null : arrivalAt.toLocalTime();
        this.endTime = arrivalAt == null ? null : arrivalAt.plusMinutes(durationMinutes).toLocalTime();
    }
}
