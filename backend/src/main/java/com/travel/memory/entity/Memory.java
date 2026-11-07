package com.travel.memory.entity;

import com.travel.common.persistence.BaseEntity;
import com.travel.media.entity.Media;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "memories")
@Getter
@NoArgsConstructor
public class Memory extends BaseEntity {

    @Version
    private long version;

    @Column(name = "client_request_id", updatable = false)
    private UUID clientRequestId;

    @Column(name = "create_request_hash", length = 64, updatable = false)
    private String createRequestHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "place_id")
    private UUID placeId;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "experienced_at")
    private Instant experiencedAt;

    @OneToMany(mappedBy = "memory", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<Media> media = new ArrayList<>();

    /**
     * Factory method to create a new Memory instance.
     */
    public static Memory create(UUID userId, UUID tripId, UUID placeId, String note, Instant experiencedAt) {
        Memory memory = new Memory();
        memory.userId = userId;
        memory.tripId = tripId;
        memory.placeId = placeId;
        memory.note = note;
        memory.experiencedAt = experiencedAt != null ? experiencedAt : Instant.now();
        return memory;
    }

    /**
     * Helper method to add a media item and set bidirectional association.
     */
    public void addMedia(Media mediaItem) {
        if (mediaItem != null) {
            this.media.add(mediaItem);
            mediaItem.setMemory(this);
        }
    }

    /**
     * Helper method to remove a media item and dissociate.
     */
    public void removeMedia(Media mediaItem) {
        if (mediaItem != null) {
            this.media.remove(mediaItem);
            mediaItem.setMemory(null);
        }
    }

    /**
     * Helper method to clear all media items.
     */
    public void clearMedia() {
        for (Media item : this.media) {
            item.setMemory(null);
        }
        this.media.clear();
    }

    public void updateNote(String note) {
        this.note = note;
    }

    public void updateExperiencedAt(Instant experiencedAt) {
        if (experiencedAt != null) {
            this.experiencedAt = experiencedAt;
        }
    }
}
