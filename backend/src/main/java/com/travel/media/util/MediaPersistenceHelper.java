package com.travel.media.util;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import com.travel.media.entity.Media;
import com.travel.media.repository.MediaDeletionRepository;
import com.travel.media.repository.MediaRepository;
import com.travel.memory.entity.Memory;
import com.travel.memory.repository.MemoryRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper chịu trách nhiệm tạo Media entity từ UploadResponse
 * và lưu xuống PostgreSQL thông qua quan hệ Memory ↔ Media.
 */
@Component
@RequiredArgsConstructor
public class MediaPersistenceHelper {

    private final MediaRepository mediaRepository;
    private final MemoryRepository memoryRepository;
    private final MediaDeletionRepository deletions;

    @Transactional(readOnly = true)
    public void validateOwner(UUID owner, UUID trip, UUID id) {
        requireOwner(
            memoryRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND)),
            owner,
            trip
        );
    }

    @Transactional(readOnly = true)
    public Optional<UploadResponse> uploaded(UUID owner, UUID trip, UUID id, String hash) {
        requireOwner(
            memoryRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND)),
            owner,
            trip
        );
        rejectRemovedUpload(id, hash);
        return mediaRepository.findByMemoryIdAndUploadHash(id, hash).map(this::response);
    }

    /** The row lock is held only for DB finalization, never while calling Cloudinary. */
    @Transactional
    public UploadResponse complete(UUID owner, UUID trip, UUID id, String hash, UploadResponse uploaded) {
        Memory memory = memoryRepository.lockActive(id).orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
        requireOwner(memory, owner, trip);
        rejectRemovedUpload(id, hash);
        var existing = mediaRepository.findByMemoryIdAndUploadHash(id, hash);
        if (existing.isPresent()) return response(existing.get());
        Media media = Media.create(
            uploaded.getUrl(),
            uploaded.getMimeType(),
            uploaded.getFileSize(),
            uploaded.getWidth(),
            uploaded.getHeight(),
            memory.getMedia().size()
        );
        media.identifyUpload(hash, uploaded.getPublicId());
        memory.addMedia(media);
        return response(mediaRepository.saveAndFlush(media));
    }

    private void requireOwner(Memory memory, UUID owner, UUID trip) {
        if (
            !memory.getUserId().equals(owner) || !java.util.Objects.equals(memory.getTripId(), trip)
        ) throw new AppException(ErrorCode.MEMORY_ACCESS_DENIED);
    }

    private void rejectRemovedUpload(UUID memory, String hash) {
        if (deletions.uploadWasRemoved(memory, hash)) throw new AppException(ErrorCode.MEDIA_UPLOAD_REMOVED);
    }

    private UploadResponse response(Media media) {
        return UploadResponse.builder()
            .mediaId(media.getId())
            .url(media.getUrl())
            .publicId(media.getPublicId())
            .mimeType(media.getMimeType())
            .fileSize(media.getFileSize())
            .width(media.getWidth())
            .height(media.getHeight())
            .build();
    }
}
