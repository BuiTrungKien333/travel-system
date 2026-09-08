package com.travel.media.controller;

import com.travel.common.response.ApiResponse;
import com.travel.media.dto.UploadResponse;
import com.travel.media.service.MediaService;
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
            @RequestParam(value = "memoryId", required = false) UUID memoryId
    ) {
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
            response = mediaService.uploadImage(file, currentUserId, tripId, memoryId);
        } else {
            response = mediaService.uploadImage(file, currentUserId);
        }
        return ApiResponse.success("Image uploaded successfully", response);
    }

    /**
     * Upload multiple images to Cloudinary (up to 5).
     * POST /api/v1/media/uploads?tripId=...&memoryId=...
     */
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload multiple images",
            description = "Upload up to 5 image files at once. Each file must be JPEG, PNG, GIF, or WebP. Max 10MB per file. "
                    + "Organized into folder structure if tripId & memoryId are provided."
    )
    public ApiResponse<List<UploadResponse>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "tripId", required = false) UUID tripId,
            @RequestParam(value = "memoryId", required = false) UUID memoryId
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        log.info("User [{}] uploading {} images, tripId={}, memoryId={}",
                currentUserId, files.size(), tripId, memoryId);

        List<UploadResponse> responses;
        if (tripId != null && memoryId != null) {
            responses = mediaService.uploadImages(files, currentUserId, tripId, memoryId);
        } else {
            responses = mediaService.uploadImages(files, currentUserId);
        }
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
