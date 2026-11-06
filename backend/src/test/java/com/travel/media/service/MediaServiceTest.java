package com.travel.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import com.travel.media.entity.Media;
import com.travel.media.repository.MediaRepository;
import com.travel.media.service.impl.MediaServiceImpl;
import com.travel.memory.entity.Memory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private ObjectProvider<Cloudinary> cloudinaryProvider;

    @Mock
    private Uploader uploader;

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private UUID userId;
    private UUID tripId;
    private UUID placeId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        placeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Delete media succeeds when owned by user")
    void deleteMedia_Success() {
        Memory memory = Memory.create(userId, tripId, placeId, "Memory with media", Instant.now());
        Media media = Media.create("https://cdn.travel.com/image.jpg", "image/jpeg", 1024L, 800, 600, 0);
        memory.addMedia(media);

        UUID mediaId = UUID.randomUUID();
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        mediaService.deleteMedia(mediaId, userId);

        assertThat(memory.getMedia()).isEmpty();
        verify(mediaRepository, times(1)).delete(media);
    }

    @Test
    @DisplayName("Delete media throws exception when media does not exist")
    void deleteMedia_NotFound() {
        UUID mediaId = UUID.randomUUID();
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.deleteMedia(mediaId, userId))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MEDIA_NOT_FOUND));
    }

    @Test
    @DisplayName("Delete media throws exception when user does not own memory")
    void deleteMedia_AccessDenied() {
        Memory memory = Memory.create(userId, tripId, placeId, "Memory with media", Instant.now());
        Media media = Media.create("https://cdn.travel.com/image.jpg", "image/jpeg", 1024L, 800, 600, 0);
        memory.addMedia(media);

        UUID mediaId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));

        assertThatThrownBy(() -> mediaService.deleteMedia(mediaId, otherUserId))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MEMORY_ACCESS_DENIED));
    }

    @Test
    @DisplayName("Upload single image succeeds")
    void uploadImage_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "dummy image content".getBytes()
        );

        Map<String, Object> cloudinaryResult = new HashMap<>();
        cloudinaryResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/test.jpg");
        cloudinaryResult.put("public_id", "travel-system/memories/test");
        cloudinaryResult.put("bytes", 18L);
        cloudinaryResult.put("width", 800);
        cloudinaryResult.put("height", 600);

        when(cloudinaryProvider.getIfAvailable()).thenReturn(cloudinary);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(cloudinaryResult);

        UploadResponse response = mediaService.uploadImage(file, userId);

        assertThat(response).isNotNull();
        assertThat(response.getUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/test.jpg");
        assertThat(response.getPublicId()).isEqualTo("travel-system/memories/test");
        assertThat(response.getMimeType()).isEqualTo("image/jpeg");
        assertThat(response.getWidth()).isEqualTo(800);
        assertThat(response.getHeight()).isEqualTo(600);
    }

    @Test
    void uploadWithoutStorageConfigurationReturnsUnavailableWithoutCallingProvider() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});
        assertThatThrownBy(() -> mediaService.uploadImage(file, userId))
                .isInstanceOfSatisfying(AppException.class, error -> {
                    assertThat(error.getErrorCode()).isEqualTo(ErrorCode.MEDIA_STORAGE_UNAVAILABLE);
                    assertThat(error.getErrorCode().getHttpStatus().value()).isEqualTo(503);
                });
        verifyNoInteractions(cloudinary, uploader, mediaRepository);
    }

    @Test
    @DisplayName("Upload image throws exception when file is empty")
    void uploadImage_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> mediaService.uploadImage(file, userId))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_FILE_EMPTY));
    }

    @Test
    @DisplayName("Upload image throws exception when file type is invalid")
    void uploadImage_InvalidType_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "dummy pdf content".getBytes()
        );

        assertThatThrownBy(() -> mediaService.uploadImage(file, userId))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_FILE_TYPE));
    }

    @Test
    @DisplayName("Upload multiple images throws exception when more than 10 files")
    void uploadImages_TooManyFiles_ThrowsException() {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            files.add(new MockMultipartFile("file", "test" + i + ".jpg", "image/jpeg", "content".getBytes()));
        }

        assertThatThrownBy(() -> mediaService.uploadImages(files, userId))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_TOO_MANY_FILES));
    }
}
