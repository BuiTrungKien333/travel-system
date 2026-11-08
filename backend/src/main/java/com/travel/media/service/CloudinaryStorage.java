package com.travel.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CloudinaryStorage {
    private final Cloudinary cloudinary;

    public UploadResponse upload(ImageUploadValidator.Image image, String publicId) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(image.bytes(), ObjectUtils.asMap(
                    "public_id", publicId, "resource_type", "image", "overwrite", false,
                    "unique_filename", false, "timeout", 30_000));
            String url = (String) result.get("secure_url");
            if (url == null || !url.startsWith("https://") || !publicId.equals(result.get("public_id")))
                throw new AppException(ErrorCode.UPLOAD_FAILED);
            return UploadResponse.builder().url(url).publicId(publicId).mimeType(image.mimeType())
                    .fileSize((long) image.bytes().length).width(image.width()).height(image.height()).build();
        } catch (IOException | RuntimeException e) {
            if (e instanceof AppException app) throw app;
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    public void destroy(String publicId) throws IOException {
        var result = cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", "image", "invalidate", true, "timeout", 30_000));
        if (!"ok".equals(result.get("result")) && !"not found".equals(result.get("result")))
            throw new IOException("Storage deletion was not acknowledged");
    }
}
