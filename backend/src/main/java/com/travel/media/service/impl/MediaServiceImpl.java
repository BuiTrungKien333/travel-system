package com.travel.media.service.impl;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import com.travel.media.repository.MediaRepository;
import com.travel.media.service.*;
import com.travel.media.util.CloudinaryPathBuilder;
import com.travel.media.util.MediaPersistenceHelper;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.MemoryAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {
    private final ImageUploadValidator validator;
    private final UploadAdmission admission;
    private final CloudinaryStorage storage;
    private final MediaPersistenceHelper persistence;
    private final MediaCleanupQueue cleanup;
    private final MediaRepository mediaRepository;
    private final MemoryRepository memories;
    private final MemoryAccess access;

    @Override
    public UploadResponse uploadImage(MultipartFile file, UUID userId, UUID tripId, UUID memoryId) {
        return uploadImages(Collections.singletonList(file), userId, tripId, memoryId).getFirst();
    }

    @Override
    public List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId, UUID tripId, UUID memoryId) {
        validator.validateBatch(files);
        var existing = persistence.existing(userId, tripId, memoryId);
        try (var permit = admission.acquire(userId)) {
            var images = new LinkedHashMap<String, ImageUploadValidator.Image>();
            for (MultipartFile file : files) {
                var image = validator.read(file);
                images.putIfAbsent(image.hash(), image);
            }
            persistence.checkCapacity(userId, tripId, memoryId, images.keySet());
            List<MediaPersistenceHelper.Uploaded> uploaded = new ArrayList<>();
            List<String> newAssets = new ArrayList<>();
            String folder = tripId == null ? CloudinaryPathBuilder.miscFolder(userId) + "/" + memoryId
                    : CloudinaryPathBuilder.memoryFolder(userId, tripId, memoryId);
            try {
                for (var image : images.values()) {
                    var response = existing.get(image.hash());
                    if (response == null) {
                        String publicId = folder + "/" + UUID.randomUUID();
                        cleanup.track(publicId);
                        newAssets.add(publicId);
                        response = storage.upload(image, publicId);
                    }
                    uploaded.add(new MediaPersistenceHelper.Uploaded(image.hash(), response));
                }
                return persistence.attach(userId, tripId, memoryId, uploaded);
            } catch (RuntimeException e) {
                for (String publicId : newAssets) {
                    try { cleanup.enqueue(publicId); }
                    catch (RuntimeException cleanupError) {
                        // The pre-upload intent still schedules cleanup if the database is temporarily unavailable.
                        log.warn("Could not expedite cleanup; upload intent retained, cause={}", cleanupError.getClass().getSimpleName());
                    }
                }
                throw e;
            }
        }
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId, UUID owner) {
        var initial = mediaRepository.findById(mediaId).orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));
        UUID memoryId = initial.getMemory().getId();
        var memory = access.lock(memoryId, owner, null);
        var media = memory.getMedia().stream().filter(m -> mediaId.equals(m.getId())).findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));
        cleanup.enqueue(media.getPublicId());
        memory.removeMedia(media);
        for (int i = 0; i < memory.getMedia().size(); i++) memory.getMedia().get(i).setDisplayOrder(i);
        memory.markChanged();
        memories.flush();
    }
}
