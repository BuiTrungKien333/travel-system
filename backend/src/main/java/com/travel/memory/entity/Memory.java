package com.travel.memory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "memory")
@Getter
@NoArgsConstructor
public class Memory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    public static Memory create(UUID userId, UUID tripId, UUID placeId,
                                String content, LocalDateTime visitedAt) {
        Memory memory = new Memory();
        memory.userId    = userId;
        memory.tripId    = tripId;
        memory.placeId   = placeId;
        memory.content   = content;
        memory.visitedAt = visitedAt;
        return memory;
    }
    public void updateContent(String content) {
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }
    public void updateVisitedAt(LocalDateTime visitedAt) {
        if (visitedAt != null) {
            this.visitedAt = visitedAt;
        }
    }
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
