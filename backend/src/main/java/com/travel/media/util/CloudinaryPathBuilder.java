package com.travel.media.util;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Utility class để xây dựng đường dẫn folder và đặt tên file
 * trước khi upload lên Cloudinary.
 *
 * Cấu trúc folder: travel-system/users/{userId}/trips/{tripId}/{memoryId}/
 * Tên file:         memory_{yyyyMMdd}_{HHmmss}_{index}.{ext}
 */
public final class CloudinaryPathBuilder {

    private static final String ROOT = "travel-system";
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private CloudinaryPathBuilder() {}
    /**
     * Folder cho memory media:
     * travel-system/users/{userId}/trips/{tripId}/{memoryId}
     */
    public static String memoryFolder(UUID userId, UUID tripId, UUID memoryId) {
        return String.format("%s/users/%s/trips/%s/%s",
                ROOT, userId, tripId, memoryId);
    }
    /**
     * Folder cho avatar:
     * travel-system/users/{userId}/avatar
     */
    public static String avatarFolder(UUID userId) {
        return String.format("%s/users/%s/avatar", ROOT, userId);
    }
    /**
     * Folder cho trip cover image:
     * travel-system/users/{userId}/trips/{tripId}/cover
     */
    public static String tripCoverFolder(UUID userId, UUID tripId) {
        return String.format("%s/users/%s/trips/%s/cover", ROOT, userId, tripId);
    }
    /**
     * Folder cho ảnh không thuộc trip nào:
     * travel-system/users/{userId}/misc
     */
    public static String miscFolder(UUID userId) {
        return String.format("%s/users/%s/misc", ROOT, userId);
    }
    /**
     * Đặt lại tên file upload theo format:
     *   memory_{yyyyMMdd}_{HHmmss}_{index}
     *
     * Cloudinary sẽ tự thêm extension dựa vào resource_type,
     * nên public_id KHÔNG cần chứa đuôi file.
     *
     * @param index vị trí file trong batch (bắt đầu từ 1)
     * @return tên file mới (không có extension)
     */
    public static String generateMemoryFileName(int index) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        return String.format("memory_%s_%03d", timestamp, index);
    }
    public static String generateAvatarFileName() {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        return String.format("avatar_%s", timestamp);
    }
    public static String extractExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        }
        String mimeType = file.getContentType();
        if (mimeType != null) {
            return switch (mimeType.toLowerCase()) {
                case "image/png"  -> "png";
                case "image/gif"  -> "gif";
                case "image/webp" -> "webp";
                default           -> "jpg";
            };
        }
        return "jpg";
    }
}
