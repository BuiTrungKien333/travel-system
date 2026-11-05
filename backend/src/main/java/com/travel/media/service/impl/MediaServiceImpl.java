package com.travel.media.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import com.travel.media.dto.UploadLimitsResponse;
import com.travel.media.entity.Media;
import com.travel.media.repository.MediaRepository;
import com.travel.media.service.MediaService;
import com.travel.media.util.CloudinaryPathBuilder;
import com.travel.media.util.MediaPersistenceHelper;
import com.travel.memory.entity.Memory;
import com.travel.memory.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final MemoryRepository memoryRepository;
    private final MediaPersistenceHelper mediaPersistenceHelper;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    @Override
    public UploadLimitsResponse limits() { return new UploadLimitsResponse(MAX_FILE_SIZE); }
    private static final int MAX_FILES_PER_REQUEST = 10;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    @Override
    @Transactional
    public UploadResponse uploadImage(MultipartFile file, UUID userId, UUID tripId, UUID memoryId) {
        validateFile(file);
        Memory memory = findOwnedMemory(userId, tripId, memoryId);
        String folder = CloudinaryPathBuilder.memoryFolder(userId, tripId, memoryId);
        String publicId = CloudinaryPathBuilder.generateMemoryFileName(1);
        UploadResponse response = doUpload(file, folder, publicId);
        return mediaPersistenceHelper.saveAndBuildResponse(response, memory, memory.getMedia().size());
    }
    @Override
    @Transactional
    public List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId, UUID tripId, UUID memoryId) {
        validateFileList(files);
        String folder = CloudinaryPathBuilder.memoryFolder(userId, tripId, memoryId);

        Memory memory = findOwnedMemory(userId, tripId, memoryId);

        int baseOrder = memory.getMedia().size();
        List<UploadResponse> responses = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            String publicId = CloudinaryPathBuilder.generateMemoryFileName(i + 1);
            UploadResponse response = doUpload(files.get(i), folder, publicId);
            responses.add(mediaPersistenceHelper.saveAndBuildResponse(response, memory, baseOrder + i));
        }
        return responses;
    }

    private Memory findOwnedMemory(UUID userId, UUID tripId, UUID memoryId) {
        Memory memory = memoryRepository.findByIdAndDeletedAtIsNull(memoryId)
                .orElseThrow(() -> new AppException(ErrorCode.MEMORY_NOT_FOUND));
        if (!memory.getUserId().equals(userId)
                || memory.getTripId() == null
                || !memory.getTripId().equals(tripId)) {
            throw new AppException(ErrorCode.MEMORY_ACCESS_DENIED);
        }
        return memory;
    }
    @Override
    public UploadResponse uploadImage(MultipartFile file, UUID userId) {
        validateFile(file);
        String folder = CloudinaryPathBuilder.miscFolder(userId);
        String publicId = CloudinaryPathBuilder.generateMemoryFileName(1);
        return doUpload(file, folder, publicId);
    }

    @Override
    public List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId) {
        validateFileList(files);
        String folder = CloudinaryPathBuilder.miscFolder(userId);

        List<UploadResponse> responses = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            String publicId = CloudinaryPathBuilder.generateMemoryFileName(i + 1);
            responses.add(doUpload(files.get(i), folder, publicId));
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
    /**
     * Upload thực tế lên Cloudinary.
     *
     * @param file     file cần upload
     * @param folder   đường dẫn folder trên Cloudinary
     * @param publicId tên file mới (không có extension)
     */
    private UploadResponse doUpload(MultipartFile file, String folder, String publicId) {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", false,
                    "unique_filename", false
            );

            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            String secureUrl = (String) result.get("secure_url");
            String resultPublicId = (String) result.get("public_id");
            Number bytes = (Number) result.get("bytes");
            Number width = (Number) result.get("width");
            Number height = (Number) result.get("height");
            String mimeType = file.getContentType();

            log.info("Image uploaded: publicId={}, folder={}, url={}, size={} bytes",
                    resultPublicId, folder, secureUrl, bytes);
            return UploadResponse.builder()
                    .url(secureUrl)
                    .publicId(resultPublicId)
                    .mimeType(mimeType)
                    .fileSize(bytes != null ? bytes.longValue() : file.getSize())
                    .width(width != null ? width.intValue() : null)
                    .height(height != null ? height.intValue() : null)
                    .build();
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: folder={}, publicId={}, fileName={}",
                    folder, publicId, file.getOriginalFilename(), e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("File is empty");
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

    private void validateFileList(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new AppException(ErrorCode.UPLOAD_TOO_MANY_FILES);
        }
        files.forEach(this::validateFile);
    }
}
