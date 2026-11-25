package com.travel.media.service.impl;

import com.cloudinary.Cloudinary;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import com.travel.media.dto.BatchUploadResponse;
import com.travel.media.service.MediaService;
import com.travel.media.service.MediaDeletionService;
import com.travel.media.util.CloudinaryPathBuilder;
import com.travel.media.util.MediaPersistenceHelper;
import com.travel.media.util.ImageUploadPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "MEDIA_SERVICE")
public class MediaServiceImpl implements MediaService {

    private final ObjectProvider<Cloudinary> cloudinaryProvider;
    private final MediaPersistenceHelper mediaPersistenceHelper;
    private final MediaDeletionService deletion;
    private static final int MAX_FILES_PER_REQUEST = 10;
    @Override
    public UploadResponse uploadImage(MultipartFile file, UUID userId, UUID tripId, UUID memoryId) {
        var image = ImageUploadPolicy.inspect(file);
        var existing = mediaPersistenceHelper.uploaded(userId, tripId, memoryId, image.hash());
        if (existing.isPresent()) return existing.get();
        String folder = CloudinaryPathBuilder.memoryFolder(userId, tripId, memoryId);
        // Stable per-memory/content ID also survives response loss between provider and DB.
        UploadResponse response = doUpload(image, folder, image.hash());
        try {
            return mediaPersistenceHelper.complete(userId, tripId, memoryId, image.hash(), response);
        } catch (AppException failure) {
            deletion.cleanLateUpload(userId, memoryId, image.hash(), response.getPublicId());
            throw failure;
        }
    }
    @Override
    public BatchUploadResponse uploadImages(List<MultipartFile> files, UUID userId, UUID tripId, UUID memoryId) {
        validateFileList(files);
        mediaPersistenceHelper.validateOwner(userId, tripId, memoryId);
        List<BatchUploadResponse.Item> results = new ArrayList<>();
        int succeeded = 0;
        for (int index = 0; index < files.size(); index++) {
            try {
                var uploaded = uploadImage(files.get(index), userId, tripId, memoryId);
                results.add(new BatchUploadResponse.Item(index, uploaded, null, null));
                succeeded++;
            } catch (AppException failure) {
                var code = failure.getErrorCode();
                results.add(new BatchUploadResponse.Item(index, null, code.getCode(), code.getHttpStatus().value()));
            } catch (RuntimeException failure) {
                log.warn("Image batch item failed ({})", failure.getClass().getSimpleName());
                results.add(new BatchUploadResponse.Item(index, null, ErrorCode.INTERNAL_SERVER_ERROR.getCode(), 500));
            }
        }
        return new BatchUploadResponse(List.copyOf(results), succeeded, files.size() - succeeded);
    }

    @Override
    public UploadResponse uploadImage(MultipartFile file, UUID userId) {
        var image = ImageUploadPolicy.inspect(file);
        String folder = CloudinaryPathBuilder.miscFolder(userId);
        String publicId = CloudinaryPathBuilder.generateMemoryFileName(1);
        return doUpload(image, folder, publicId);
    }

    @Override
    public UploadResponse uploadPlaceImage(MultipartFile file) {
        var image = ImageUploadPolicy.inspect(file);
        return doUpload(image, CloudinaryPathBuilder.placeImageFolder(), CloudinaryPathBuilder.generatePlaceImageFileName());
    }

    @Override
    public void deleteMedia(UUID mediaId, UUID requestUserId) {
        deletion.delete(mediaId, requestUserId);
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

    private void validateFileList(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new AppException(ErrorCode.UPLOAD_TOO_MANY_FILES);
        }
    }
}
