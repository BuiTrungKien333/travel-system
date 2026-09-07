package com.travel.media.service;

import com.travel.media.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {

    /**
     * Upload a single image file to Cloudinary.
     *
     * @param file   the multipart file from client
     * @param userId the user ID (used for folder organization on Cloudinary)
     * @return UploadResponse containing the secure URL and metadata
     */
    UploadResponse uploadImage(MultipartFile file, UUID userId);

    /**
     * Upload multiple image files to Cloudinary (up to 5).
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
