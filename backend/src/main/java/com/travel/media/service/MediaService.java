package com.travel.media.service;

import com.travel.media.dto.UploadResponse;
import com.travel.media.dto.UploadLimitsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {
    UploadLimitsResponse limits();

    /**
     * Upload a single image file to Cloudinary into organized folder:
     * travel-system/users/{userId}/trips/{tripId}/{memoryId}/
     *
     * @param file     the multipart file from client
     * @param userId   the user ID
     * @param tripId   the trip ID (used for folder organization)
     * @param memoryId the memory ID (used for folder organization)
     * @return UploadResponse containing the secure URL and metadata
     */
    UploadResponse uploadImage(MultipartFile file, UUID userId, UUID tripId, UUID memoryId);

    /**
     * Upload multiple image files to Cloudinary (up to 10) into organized folder.
     *
     * @param files    the list of multipart files
     * @param userId   the user ID
     * @param tripId   the trip ID
     * @param memoryId the memory ID
     * @return list of UploadResponse
     */
    List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId, UUID tripId, UUID memoryId);

    /**
     * Upload a single image file to Cloudinary (misc folder — backward compatible).
     *
     * @param file   the multipart file from client
     * @param userId the user ID (used for folder organization on Cloudinary)
     * @return UploadResponse containing the secure URL and metadata
     */
    UploadResponse uploadImage(MultipartFile file, UUID userId);

    /**
     * Upload multiple image files to Cloudinary (up to 10, misc folder — backward compatible).
     *
     * @param files  the list of multipart files
     * @param userId the user ID
     * @return list of UploadResponse
     */
    List<UploadResponse> uploadImages(List<MultipartFile> files, UUID userId);

    /**
     * Delete a specific Media entity from a memory. Validates ownership.
     *
     * @param mediaId       the ID of the media item to delete
     * @param requestUserId the ID of the user requesting deletion
     */
    void deleteMedia(UUID mediaId, UUID requestUserId);

    /**
     * Delete an image from Cloudinary by its publicId.
     *
     * @param publicId the Cloudinary publicId
     */
    void deleteCloudinaryImage(String publicId);
}
