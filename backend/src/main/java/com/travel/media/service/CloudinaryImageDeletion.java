package com.travel.media.service;

import com.cloudinary.Cloudinary;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageDeletion {

    private final ObjectProvider<Cloudinary> provider;

    public void destroy(String publicId) {
        if (publicId == null) return; // Legacy external link, not an owned storage asset.
        if (publicId.isBlank()) throw new AppException(ErrorCode.MEDIA_DELETE_IDENTITY_MISSING);
        var cloudinary = provider.getIfAvailable();
        if (cloudinary == null) throw new AppException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
        try {
            var result = cloudinary.uploader().destroy(publicId, Map.of("resource_type", "image", "invalidate", true));
            if (
                result == null || !("ok".equals(result.get("result")) || "not found".equals(result.get("result")))
            ) throw new AppException(ErrorCode.MEDIA_DELETE_FAILED);
        } catch (AppException error) {
            throw error;
        } catch (Exception error) {
            log.warn("Image provider deletion failed ({})", error.getClass().getSimpleName());
            throw new AppException(ErrorCode.MEDIA_DELETE_FAILED);
        }
    }
}
