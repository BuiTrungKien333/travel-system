package com.travel.media.service.impl;

import com.cloudinary.Cloudinary;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import com.travel.media.entity.Media;
import com.travel.media.repository.MediaRepository;
import com.travel.media.service.MediaService;
import com.travel.media.util.CloudinaryPathBuilder;
import com.travel.media.util.MediaPersistenceHelper;
import com.travel.media.util.ImageUploadPolicy;
import com.travel.memory.entity.Memory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
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

    private final ObjectProvider<Cloudinary> cloudinaryProvider;
    private final MediaRepository mediaRepository;
    private final MediaPersistenceHelper mediaPersistenceHelper;
    private static final int MAX_FILES_PER_REQUEST = 10;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
    @Override
    public UploadResponse uploadImage(MultipartFile file, UUID userId, UUID tripId, UUID memoryId) {
        var image = ImageUploadPolicy.inspect(file);
        var existing = mediaPersistenceHelper.uploaded(userId, tripId, memoryId, image.hash());
        if (existing.isPresent()) return existing.get();
        String folder = CloudinaryPathBuilder.memoryFolder(userId, tripId, memoryId);
        // Stable per-memory/content ID also survives response loss between provider and DB.
        UploadResponse response = doUpload(image, folder, image.hash());
        return mediaPersistenceHelper.complete(userId, tripId, memoryId, image.hash(), response);
    }
    @Override
    public List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId, UUID tripId, UUID memoryId) {
        validateFileList(files);
        List<UploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) responses.add(uploadImage(file, userId, tripId, memoryId));
        return responses;
    }

    @Override
    public UploadResponse uploadImage(MultipartFile file, UUID userId) {
        var image = ImageUploadPolicy.inspect(file);
        String folder = CloudinaryPathBuilder.miscFolder(userId);
        String publicId = CloudinaryPathBuilder.generateMemoryFileName(1);
        return doUpload(image, folder, publicId);
    }

    @Override
    public List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId) {
        validateFileList(files);
        String folder = CloudinaryPathBuilder.miscFolder(userId);

        List<UploadResponse> responses = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            String publicId = CloudinaryPathBuilder.generateMemoryFileName(i + 1);
            responses.add(doUpload(ImageUploadPolicy.inspect(files.get(i)), folder, publicId));
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
        Cloudinary cloudinary = requireCloudinary();
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, Map.of());
            log.info("Cloudinary delete result for publicId [{}]: {}", publicId, result.get("result"));
        } catch (IOException e) {
            log.warn("Cloudinary deletion failed");
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }
    private Cloudinary requireCloudinary() {
        Cloudinary client = cloudinaryProvider.getIfAvailable();
        if (client == null) throw new AppException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
        return client;
    }

    /**
     * Upload thực tế lên Cloudinary.
     *
     * @param image    validated image bytes and dimensions
     * @param folder   đường dẫn folder trên Cloudinary
     * @param publicId tên file mới (không có extension)
     */
    private UploadResponse doUpload(ImageUploadPolicy.Image image, String folder, String publicId) {
        Cloudinary cloudinary = requireCloudinary();
        try {
            Map<String, Object> uploadParams = Map.of(
                    "folder", folder,
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", false,
                    "unique_filename", false
            );

            Map<?, ?> result = cloudinary.uploader().upload(image.bytes(), uploadParams);
            // Some existing-asset responses omit metadata; recover it without uploading another object.
            if (Boolean.TRUE.equals(result.get("existing")) && !(result.get("secure_url") instanceof String))
                result = cloudinary.api().resource(folder + "/" + publicId, Map.of("resource_type", "image"));

            String secureUrl = (String) result.get("secure_url");
            String resultPublicId = (String) result.get("public_id");
            Number bytes = (Number) result.get("bytes");
            Number width = (Number) result.get("width");
            Number height = (Number) result.get("height");
            String mimeType = image.mimeType();
            if (secureUrl == null || !secureUrl.startsWith("https://") || resultPublicId == null)
                throw new AppException(ErrorCode.UPLOAD_FAILED);

            log.info("Image uploaded: size={} bytes", bytes);
            return UploadResponse.builder()
                    .url(secureUrl)
                    .publicId(resultPublicId)
                    .mimeType(mimeType)
                    .fileSize(bytes != null ? bytes.longValue() : (long) image.bytes().length)
                    .width(width != null ? width.intValue() : image.width())
                    .height(height != null ? height.intValue() : image.height())
                    .build();
        } catch (AppException error) {
            throw error;
        } catch (Exception error) {
            log.warn("Image provider upload failed ({})", error.getClass().getSimpleName());
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
        if (file.getSize() > ImageUploadPolicy.MAX_FILE_BYTES) {
            log.warn("File too large: {} bytes", file.getSize());
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
