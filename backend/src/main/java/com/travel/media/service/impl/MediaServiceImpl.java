package com.travel.media.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import com.travel.media.entity.Media;
import com.travel.media.repository.MediaRepository;
import com.travel.media.service.MediaService;
import com.travel.memory.entity.Memory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "MEDIA_SERVICE")
public class MediaServiceImpl implements MediaService {

    private final Cloudinary cloudinary;
    private final MediaRepository mediaRepository;

    @Value("${app.cloudinary.folder:travel-system/memories}")
    private String baseFolder;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_FILES_PER_REQUEST = 5;

    @Override
    public UploadResponse uploadImage(MultipartFile file, UUID userId) {
        validateFile(file);
        return doUpload(file, userId);
    }


    @Override
    public List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId) {
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new AppException(ErrorCode.UPLOAD_TOO_MANY_FILES);
        }

        files.forEach(this::validateFile);

        List<UploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(doUpload(file, userId));
        }
        return responses;
    }

    @Override
    @Transactional
    public void deleteMedia(UUID mediaId, UUID requestUserId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDIA_NOT_FOUND));

        Memory memory = media.getMemory();
        if (memory == null || memory.isDeleted() || !memory.getUserId().equals(requestUserId)) {
            throw new AppException(ErrorCode.MEMORY_ACCESS_DENIED);
        }

        memory.removeMedia(media);
        mediaRepository.delete(media);
        log.info("Media deleted: mediaId={}, memoryId={}, userId={}", mediaId, memory.getId(), requestUserId);
    }

    @Override
    public void deleteCloudinaryImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete result for publicId [{}]: {}", publicId, result.get("result"));
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: publicId={}", publicId, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.UPLOAD_FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Invalid file type attempted: {}", contentType);
            throw new AppException(ErrorCode.UPLOAD_INVALID_FILE_TYPE);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File too large: {} bytes (max: {} bytes)", file.getSize(), MAX_FILE_SIZE);
            throw new AppException(ErrorCode.UPLOAD_FILE_TOO_LARGE);
        }
    }

    private UploadResponse doUpload(MultipartFile file, UUID userId) {
        try {
            String folder = baseFolder + "/" + userId.toString();

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "overwrite", false,
                    "unique_filename", true
            );

            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            Number bytes = (Number) result.get("bytes");
            Number width = (Number) result.get("width");
            Number height = (Number) result.get("height");
            String mimeType = file.getContentType();

            log.info("Image uploaded successfully: publicId={}, url={}, size={} bytes",
                    publicId, secureUrl, bytes);

            return UploadResponse.builder()
                    .url(secureUrl)
                    .publicId(publicId)
                    .mimeType(mimeType)
                    .fileSize(bytes != null ? bytes.longValue() : file.getSize())
                    .width(width != null ? width.intValue() : null)
                    .height(height != null ? height.intValue() : null)
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: userId={}, fileName={}",
                    userId, file.getOriginalFilename(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }
}
