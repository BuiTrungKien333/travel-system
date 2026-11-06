package com.travel.media.util;

import com.travel.media.dto.UploadResponse;
import com.travel.media.entity.Media;
import com.travel.media.repository.MediaRepository;
import com.travel.memory.entity.Memory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Helper chịu trách nhiệm tạo Media entity từ UploadResponse
 * và lưu xuống PostgreSQL thông qua quan hệ Memory ↔ Media.
 */
@Component
@RequiredArgsConstructor
@Slf4j(topic = "MEDIA_PERSISTENCE")
public class MediaPersistenceHelper {

    private final MediaRepository mediaRepository;

    /**
     * Tạo Media entity từ kết quả upload Cloudinary, gắn vào Memory và persist xuống DB.
     *
     * @param response     kết quả trả về từ Cloudinary upload
     * @param memory       Memory entity sở hữu ảnh này
     * @param displayOrder thứ tự hiển thị trong memory
     * @return UploadResponse bổ sung thêm mediaId từ DB
     */
    public UploadResponse saveAndBuildResponse(UploadResponse response, Memory memory, int displayOrder) {
        return saveAndBuildResponse(response, memory, displayOrder, null, null);
    }
    public UploadResponse saveAndBuildResponse(UploadResponse response, Memory memory, int displayOrder,
                                              java.util.UUID clientRequestId, String digest) {
        Media media = Media.create(
                response.getUrl(),
                response.getMimeType(),
                response.getFileSize(),
                response.getWidth(),
                response.getHeight(),
                displayOrder
        );
        memory.addMedia(media);
        media.setClientRequestId(clientRequestId);
        media.setUploadSha256(digest);
        mediaRepository.save(media);

        log.info("Media saved to DB: mediaId={}, memoryId={}, url={}",
                media.getId(), memory.getId(), response.getUrl());

        return UploadResponse.builder()
                .mediaId(media.getId())
                .url(response.getUrl())
                .publicId(response.getPublicId())
                .mimeType(response.getMimeType())
                .fileSize(response.getFileSize())
                .width(response.getWidth())
                .height(response.getHeight())
                .build();
    }
}
