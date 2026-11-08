package com.travel.media.controller;

import com.travel.common.response.ApiResponse;
import com.travel.media.dto.UploadResponse;
import com.travel.media.service.MediaService;
import com.travel.media.config.MediaProperties;
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
    private final MediaProperties limits;

    @GetMapping("/limits")
    @Operation(summary = "Get upload limits for image selection and validation")
    public ApiResponse<MediaProperties> limits() { return ApiResponse.success(limits); }
    /**
     * Upload a single image to Cloudinary.
     * Requires a memoryId; an optional tripId must match its parent trip.
     *   travel-system/users/{userId}/trips/{tripId}/{memoryId}/
     * POST /api/v1/media/upload?tripId=...&memoryId=...
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload single image",
            description = "Upload a single image file (JPEG, PNG, GIF, WebP) to Cloudinary. Max size: 10MB. "
                    + "memoryId is required; tripId is optional and must match the memory. "
                    + "Actual file contents are verified; identical bytes in the same memory reuse existing media."
    )
    public ApiResponse<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tripId", required = false) UUID tripId,
            @RequestParam("memoryId") UUID memoryId
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] uploading image: name={}, type={}, size={} bytes, tripId={}, memoryId={}",
                currentUserId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                tripId,
                memoryId);

        UploadResponse response = mediaService.uploadImage(file, currentUserId, tripId, memoryId);
        return ApiResponse.success("Image uploaded successfully", response);
    }

    /**
     * Upload multiple images to Cloudinary (up to 10).
     * POST /api/v1/media/uploads?tripId=...&memoryId=...
     */
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload multiple images",
            description = "Upload up to 10 image files at once. Each file must be JPEG, PNG, GIF, or WebP. Max 10MB per file. "
                    + "Requires memoryId. The batch is attached atomically; quota is checked before storage and again under a row lock."
    )
    public ApiResponse<List<UploadResponse>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "tripId", required = false) UUID tripId,
            @RequestParam("memoryId") UUID memoryId
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] uploading {} images, tripId={}, memoryId={}",
                currentUserId, files.size(), tripId, memoryId);

        List<UploadResponse> responses = mediaService.uploadImages(files, currentUserId, tripId, memoryId);
        return ApiResponse.success("Images uploaded successfully", responses);
    }
    /**
     * Delete a specific media item by ID.
     * DELETE /api/v1/media?id=... (alias: /delete-media)
     */
    @DeleteMapping(value = {"", "/delete-media"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete media item",
            description = "Permanently deletes a specific media item from a memory. Requires ownership."
    )
    public void deleteMedia(@RequestParam("id") UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] is deleting media [{}]", currentUserId, id);
        mediaService.deleteMedia(id, currentUserId);
    }
}
