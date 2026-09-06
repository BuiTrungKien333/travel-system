package com.travel.memory.repository;

import com.travel.memory.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    /** Retrieve all media items for a memory ordered by displayOrder ascending. */
    List<Media> findByMemoryIdOrderByDisplayOrderAsc(UUID memoryId);

    /** Delete all media items belonging to a specific memory ID. */
    void deleteByMemoryId(UUID memoryId);
}
