package com.travel.media.service;

import com.travel.media.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface MediaService {
    UploadResponse uploadImage(MultipartFile file, UUID userId, UUID tripId, UUID memoryId);
    List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId, UUID tripId, UUID memoryId);
    void deleteMedia(UUID mediaId, UUID requestUserId);
}
