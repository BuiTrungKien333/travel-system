package com.travel.media.util;

import com.travel.media.dto.UploadResponse;
import com.travel.media.entity.Media;
import com.travel.media.repository.MediaRepository;
import com.travel.media.repository.MediaDeletionRepository;
import com.travel.memory.entity.Memory;
import com.travel.memory.repository.MemoryRepository;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.*;

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
    private final com.travel.memory.service.MemoryAccess access;
    private final com.travel.media.config.MediaProperties limits;
    private final com.travel.media.service.MediaCleanupQueue cleanup;

    @Transactional(readOnly = true)
    public void validateOwner(UUID owner, UUID trip, UUID id) {
        requireOwner(memoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND)), owner, trip);
    }

    @Transactional(readOnly = true)
    public Optional<UploadResponse> uploaded(UUID owner, UUID trip, UUID id, String hash) {
        requireOwner(memoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND)), owner, trip);
        rejectRemovedUpload(id, hash);
        return mediaRepository.findByMemoryIdAndUploadHash(id, hash).map(MediaPersistenceHelper::response);
    }

    /** The row lock is held only for DB finalization, never while calling Cloudinary. */
    @Transactional
    public UploadResponse complete(UUID owner, UUID trip, UUID id, String hash, UploadResponse uploaded) {
        Memory memory = memoryRepository.lockActive(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
        requireOwner(memory, owner, trip);
        rejectRemovedUpload(id, hash);
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

    private void rejectRemovedUpload(UUID memory, String hash) {
        if (deletions.uploadWasRemoved(memory, hash)) throw new AppException(ErrorCode.MEDIA_UPLOAD_REMOVED);
    }

    public static UploadResponse response(Media media) {
        return UploadResponse.builder().mediaId(media.getId()).url(media.getUrl()).publicId(media.getPublicId())
                .mimeType(media.getMimeType()).fileSize(media.getFileSize()).width(media.getWidth()).height(media.getHeight()).build();
    }

    public record Uploaded(String hash, UploadResponse response) { }

    @Transactional(readOnly = true)
    public Map<String, UploadResponse> existing(UUID owner, UUID trip, UUID memoryId) {
        var memory = access.read(memoryId, owner, trip);
        Map<String, UploadResponse> result = new HashMap<>();
        for (Media media : memory.getMedia()) if (media.getContentHash() != null)
            result.put(media.getContentHash(), response(media));
        return result;
    }

    @Transactional(readOnly = true)
    public void checkCapacity(UUID owner, UUID trip, UUID memoryId, Set<String> hashes) {
        var memory = access.read(memoryId, owner, trip);
        var existing = memory.getMedia().stream().map(Media::getContentHash).collect(java.util.stream.Collectors.toSet());
        assertCapacity(memory.getMedia().size(), hashes.stream().filter(hash -> !existing.contains(hash)).count());
    }

    private void assertCapacity(int count, long additions) {
        if (additions > 0 && count + additions > limits.getMaxFilesPerMemory()) throw new AppException(ErrorCode.UPLOAD_MEMORY_LIMIT);
    }

    @Transactional
    public List<UploadResponse> attach(UUID owner, UUID trip, UUID memoryId, List<Uploaded> uploaded) {
        var memory = access.lock(memoryId, owner, trip);
        Map<String, Media> existing = new HashMap<>();
        for (Media media : memory.getMedia()) if (media.getContentHash() != null) existing.put(media.getContentHash(), media);
        long additions = uploaded.stream().map(Uploaded::hash).distinct().filter(hash -> !existing.containsKey(hash)).count();
        assertCapacity(memory.getMedia().size(), additions);
        int order = memory.getMedia().stream().mapToInt(Media::getDisplayOrder).max().orElse(-1) + 1;
        for (Uploaded upload : uploaded) {
            rejectRemovedUpload(memoryId, upload.hash());
            Media media = existing.get(upload.hash());
            if (media == null) {
                var r = upload.response();
                if (r.getMediaId() != null) throw new AppException(ErrorCode.MEMORY_VERSION_CONFLICT);
                media = Media.create(r.getUrl(), r.getMimeType(), r.getFileSize(), r.getWidth(), r.getHeight(), order++);
                media.setPublicId(r.getPublicId()); media.setContentHash(upload.hash());
                memory.addMedia(media); existing.put(upload.hash(), media);
                cleanup.retain(r.getPublicId());
            } else if (!Objects.equals(media.getPublicId(), upload.response().getPublicId())) {
                cleanup.enqueue(upload.response().getPublicId());
            }
        }
        if (additions > 0) memory.markChanged();
        memoryRepository.flush();
        return uploaded.stream().map(u -> response(existing.get(u.hash()))).toList();
    }


}
