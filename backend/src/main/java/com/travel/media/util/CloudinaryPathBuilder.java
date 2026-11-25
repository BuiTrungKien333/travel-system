package com.travel.media.util;

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
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private CloudinaryPathBuilder() {}

    /**
     * Folder cho memory media:
     * travel-system/users/{userId}/trips/{tripId}/{memoryId}
     */
    public static String memoryFolder(UUID userId, UUID tripId, UUID memoryId) {
        return String.format("%s/users/%s/trips/%s/%s", ROOT, userId, tripId, memoryId);
    }

    /**
     * Folder cho ảnh không thuộc trip nào:
     * travel-system/users/{userId}/misc
     */
    public static String miscFolder(UUID userId) {
        return String.format("%s/users/%s/misc", ROOT, userId);
    }

    public static String placeImageFolder() {
        return ROOT + "/places";
    }

    public static String generatePlaceImageFileName() {
        return "place_" + UUID.randomUUID();
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
}
