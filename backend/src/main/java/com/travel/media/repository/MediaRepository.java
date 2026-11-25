package com.travel.media.repository;

import com.travel.media.entity.Media;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, UUID> {
    java.util.Optional<Media> findByMemoryIdAndUploadHash(UUID memoryId, String uploadHash);

    List<Media> findByMemoryIdOrderByDisplayOrderAsc(UUID memoryId);

    void deleteByMemoryId(UUID memoryId);
}
