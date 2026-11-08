package com.travel.media.util;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.config.MediaProperties;
import com.travel.media.dto.UploadResponse;
import com.travel.media.entity.Media;
import com.travel.media.service.MediaCleanupQueue;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.MemoryAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Short transactions only; storage uploads happen before attaching the completed batch. */
@Component
@RequiredArgsConstructor
public class MediaPersistenceHelper {
    private final MemoryAccess access;
    private final MemoryRepository memories;
    private final MediaProperties limits;
    private final MediaCleanupQueue cleanup;

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
        memories.flush();
        return uploaded.stream().map(u -> response(existing.get(u.hash()))).toList();
    }

    public static UploadResponse response(Media media) {
        return UploadResponse.builder().mediaId(media.getId()).url(media.getUrl()).publicId(media.getPublicId())
                .mimeType(media.getMimeType()).fileSize(media.getFileSize()).width(media.getWidth()).height(media.getHeight()).build();
    }
}
