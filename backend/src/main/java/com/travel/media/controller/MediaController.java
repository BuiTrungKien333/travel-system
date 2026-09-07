package com.travel.media.controller;

import com.cloudinary.api.exceptions.BadRequest;
import com.travel.common.response.ApiResponse;
import com.travel.media.dto.UploadResponse;
import com.travel.media.service.MediaService;
import com.travel.security.model.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * POST /api/v1/media/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload single image",
            description = "Upload a single image file (JPEG, PNG, GIF, WebP) to Cloudinary. Max size: 10MB."
    )
    public ApiResponse<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) throws BadRequest {
        if (file.isEmpty()){
            throw new BadRequest("File cannot be empty");
        }
        UUID userId = currentUser.getId();
        log.info("User [{}] uploading single image: name={},type={}, size={} bytes",
                userId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize());

        UploadResponse response = mediaService.uploadImage(file, userId);
        return ApiResponse.success("Image uploaded successfully", response);
    }


    /**
     * Upload multiple images to Cloudinary (up to 5).
     * POST /api/v1/media/uploads
     */
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload multiple images",
            description = "Upload up to 5 image files at once. Each file must be JPEG, PNG, GIF, or WebP. Max 10MB per file."
    )
    public ApiResponse<List<UploadResponse>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID userId = currentUser.getId();
        log.info("User [{}] uploading {} images", userId, files.size());

        List<UploadResponse> responses = mediaService.uploadImages(files, userId);
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
    public void deleteMedia(
            @RequestParam("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID currentUserId = currentUser.getId();
        log.info("User [{}] is deleting media [{}]", currentUserId, id);
        mediaService.deleteMedia(id, currentUserId);
    }
}
