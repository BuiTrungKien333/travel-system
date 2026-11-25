package com.travel.trip.entity;

import com.travel.place.entity.Place;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "trip_places",
    uniqueConstraints = { @UniqueConstraint(name = "uk_trip_places", columnNames = { "trip_id", "place_id" }) }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "estimated_duration")
    private Integer estimatedDuration;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "planned_order")
    private Integer plannedOrder;

    @Column(name = "visited", nullable = false)
    @Builder.Default
    private boolean visited = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripPlace tripPlace = (TripPlace) o;
        return id != null && Objects.equals(id, tripPlace.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
