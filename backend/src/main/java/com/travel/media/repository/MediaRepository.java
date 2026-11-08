package com.travel.media.repository;

import com.travel.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    java.util.Optional<Media> findByMemoryIdAndUploadHash(UUID memoryId, String uploadHash);

    List<Media> findByMemoryIdOrderByDisplayOrderAsc(UUID memoryId);

    void deleteByMemoryId(UUID memoryId);
}
