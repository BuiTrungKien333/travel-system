package com.travel.media.util;

import com.travel.media.dto.UploadResponse;
import com.travel.media.entity.Media;
import com.travel.media.repository.MediaRepository;
import com.travel.memory.entity.Memory;
import com.travel.memory.repository.MemoryRepository;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.Optional;

/**
 * Helper chịu trách nhiệm tạo Media entity từ UploadResponse
 * và lưu xuống PostgreSQL thông qua quan hệ Memory ↔ Media.
 */
@Component
@RequiredArgsConstructor
public class MediaPersistenceHelper {

    private final MediaRepository mediaRepository;
    private final MemoryRepository memoryRepository;

    @Transactional(readOnly = true)
    public Optional<UploadResponse> uploaded(UUID owner, UUID trip, UUID id, String hash) {
        requireOwner(memoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND)), owner, trip);
        return mediaRepository.findByMemoryIdAndUploadHash(id, hash).map(this::response);
    }

    /** The row lock is held only for DB finalization, never while calling Cloudinary. */
    @Transactional
    public UploadResponse complete(UUID owner, UUID trip, UUID id, String hash, UploadResponse uploaded) {
        Memory memory = memoryRepository.lockActive(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
        requireOwner(memory, owner, trip);
        var existing = mediaRepository.findByMemoryIdAndUploadHash(id, hash);
        if (existing.isPresent()) return response(existing.get());
        Media media = Media.create(uploaded.getUrl(), uploaded.getMimeType(), uploaded.getFileSize(),
                uploaded.getWidth(), uploaded.getHeight(), memory.getMedia().size());
        media.identifyUpload(hash, uploaded.getPublicId());
        memory.addMedia(media);
        return response(mediaRepository.saveAndFlush(media));
    }

    private void requireOwner(Memory memory, UUID owner, UUID trip) {
        if (!memory.getUserId().equals(owner) || !java.util.Objects.equals(memory.getTripId(), trip))
            throw new AppException(ErrorCode.MEMORY_ACCESS_DENIED);
    }

    private UploadResponse response(Media media) {
        return UploadResponse.builder().mediaId(media.getId()).url(media.getUrl()).publicId(media.getPublicId())
                .mimeType(media.getMimeType()).fileSize(media.getFileSize()).width(media.getWidth()).height(media.getHeight()).build();
    }

}
