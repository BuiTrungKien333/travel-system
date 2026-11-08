package com.travel.media.controller;

import com.travel.common.response.ApiResponse;
import com.travel.media.dto.UploadResponse;
import com.travel.media.dto.BatchUploadResponse;
import com.travel.media.service.MediaService;
import com.travel.media.util.ImageUploadPolicy;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j(topic = "MEDIA_CONTROLLER")
@Tag(name = "Media Management", description = "Endpoints for uploading and managing media items")
public class MediaController {

    private final MediaService mediaService;
    private final com.travel.media.config.MediaProperties limits;

    @GetMapping("/limits")
    public ApiResponse<com.travel.media.config.MediaProperties> limits() {
        return ApiResponse.success(limits);
    }
    /**
     * Upload a single image to Cloudinary.
     * If tripId & memoryId are provided, stores in organized folder:
     *   travel-system/users/{userId}/trips/{tripId}/{memoryId}/
     * Otherwise stores in misc folder:
     *   travel-system/users/{userId}/misc/
     *
     * POST /api/v1/media/upload?tripId=...&memoryId=...
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload single image",
            description = "Upload a single image file (JPEG, PNG, GIF, WebP) to Cloudinary. Max size: 10MB. "
                    + "If tripId & memoryId are provided, the image is stored in an organized folder structure. "
                    + "Otherwise it goes to the user's misc folder."
    )
    public ApiResponse<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tripId", required = false) UUID tripId,
            @RequestParam(value = "memoryId", required = false) UUID memoryId,
            @RequestParam(value = "clientRequestId", required = false) UUID clientRequestId
    ) {
        if ((tripId == null) != (memoryId == null)) throw new AppException(ErrorCode.VALIDATION_ERROR);
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] uploading image: name={}, type={}, size={} bytes, tripId={}, memoryId={}",
                currentUserId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                tripId,
                memoryId);

        UploadResponse response;
        if (tripId != null && memoryId != null) {
            response = mediaService.uploadImage(file, currentUserId, tripId, memoryId, clientRequestId);
        } else {
            if (clientRequestId != null) throw new AppException(ErrorCode.VALIDATION_ERROR);
            response = mediaService.uploadImage(file, currentUserId);
        }
        return ApiResponse.success("Image uploaded successfully", response);
    }

    /**
     * Upload multiple images to Cloudinary (up to 10).
     * POST /api/v1/media/uploads?tripId=...&memoryId=...
     */
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Upload multiple images",
            description = "Processes up to 10 images in submission order for an owned memory and trip. Returns per-file media or errorCode/errorStatus with original index; successful files remain saved even if others fail. Retrying identical files in the same memory is idempotent. Max 10MB per file."
    )
    public ApiResponse<BatchUploadResponse> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("tripId") UUID tripId,
            @RequestParam("memoryId") UUID memoryId
    ) {
        if ((tripId == null) != (memoryId == null)) throw new AppException(ErrorCode.VALIDATION_ERROR);
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] uploading {} images, tripId={}, memoryId={}",
                currentUserId, files.size(), tripId, memoryId);

        return ApiResponse.success("Image batch processed", mediaService.uploadImages(files, currentUserId, tripId, memoryId));
    }
    /**
     * Delete a specific media item by ID.
     * DELETE /api/v1/media?id=... (alias: /delete-media)
     */
    @DeleteMapping(value = {"", "/delete-media"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete media item",
            description = "Owner-only. Confirms provider deletion and requests CDN invalidation before removing the database photo. Repeated requests are idempotent; failed cleanup is retained for retry. CDN propagation is asynchronous."
    )
    public void deleteMedia(@RequestParam("id") UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] is deleting media [{}]", currentUserId, id);
        mediaService.deleteMedia(id, currentUserId);
    }
    @PostMapping(value = "/uploads/atomic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a batch atomically", description = "Attaches every validated image together; a failed upload queues all attempted assets for cleanup.")
    public ApiResponse<List<UploadResponse>> uploadAtomic(@RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "tripId", required = false) UUID tripId, @RequestParam("memoryId") UUID memoryId) {
        return ApiResponse.success(mediaService.uploadImagesAtomic(files, SecurityUtils.getCurrentUserId(), tripId, memoryId));
    }
}
