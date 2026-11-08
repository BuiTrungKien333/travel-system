package com.travel.media.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.config.MediaProperties;
import com.travel.media.dto.UploadResponse;
import com.travel.media.repository.MediaRepository;
import com.travel.media.service.impl.MediaServiceImpl;
import com.travel.media.util.MediaPersistenceHelper;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.MemoryAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtomicMediaServiceTest {
    @Mock CloudinaryStorage storage;
    @Mock MediaPersistenceHelper persistence;
    @Mock MediaCleanupQueue cleanup;
    @Mock MediaRepository media;
    @Mock MemoryRepository memories;
    @Mock MemoryAccess access;
    MediaProperties limits;
    MediaServiceImpl service;
    UUID owner = UUID.randomUUID(), memory = UUID.randomUUID();

    @BeforeEach void setup() {
        limits = new MediaProperties();
        service = new MediaServiceImpl(null, persistence, null, new ImageUploadValidator(limits), new UploadAdmission(limits, Clock.systemUTC()),
                storage, cleanup, media, memories, access);
    }

    @Test void validatesPixelsBeforeCallingStorage() throws Exception {
        when(persistence.existing(owner, null, memory)).thenReturn(Map.of());
        when(storage.upload(any(), anyString())).thenAnswer(i -> UploadResponse.builder().url("https://example.com/photo.png").publicId(i.getArgument(1)).build());
        var expected = List.of(UploadResponse.builder().mediaId(UUID.randomUUID()).url("https://example.com/photo.png").build());
        when(persistence.attach(eq(owner), isNull(), eq(memory), anyList())).thenReturn(expected);
        assertThat(service.uploadImagesAtomic(List.of(png(1)), owner, null, memory).getFirst().getMediaId()).isEqualTo(expected.getFirst().getMediaId());
        verify(storage).upload(argThat(image -> image.width() == 2 && image.height() == 2 && image.mimeType().equals("image/png")), anyString());
    }

    @Test void partialProviderFailureQueuesEveryAttemptedAssetWithoutAttaching() throws Exception {
        when(persistence.existing(owner, null, memory)).thenReturn(Map.of());
        when(storage.upload(any(), anyString())).thenAnswer(i -> UploadResponse.builder().publicId(i.getArgument(1)).build())
                .thenThrow(new AppException(ErrorCode.UPLOAD_FAILED));
        assertThatThrownBy(() -> service.uploadImagesAtomic(List.of(png(1), png(2)), owner, null, memory)).isInstanceOf(AppException.class);
        verify(cleanup, times(2)).track(anyString());
        verify(cleanup, times(2)).enqueue(anyString());
        verify(persistence, never()).attach(any(), any(), any(), any());
    }

    @Test void rejectsInvalidImageEvenWhenMimeClaimsPng() {
        when(persistence.existing(owner, null, memory)).thenReturn(Map.of());
        assertThatThrownBy(() -> service.uploadImagesAtomic(List.of(new MockMultipartFile("files", "x.png", "image/png", "not an image".getBytes())), owner, null, memory))
                .isInstanceOf(AppException.class).extracting(e -> ((AppException)e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_FILE_TYPE);
        verifyNoInteractions(storage);
    }

    @Test void rejectsTooManyFilesAndLargeBatchesBeforeProviderCalls() throws Exception {
        var file = png(1);
        assertThatThrownBy(() -> service.uploadImagesAtomic(Collections.nCopies(11, file), owner, null, memory)).isInstanceOf(AppException.class);
        limits.setMaxBatchBytes(1);
        assertThatThrownBy(() -> service.uploadImagesAtomic(List.of(file), owner, null, memory)).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException)e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_BATCH_TOO_LARGE);
        verifyNoInteractions(storage, persistence);
    }

    @Test void preventsDuplicateStorageUploadsForTheSameContent() throws Exception {
        var file = png(1);
        String hash = new ImageUploadValidator(limits).read(file).hash();
        var existing = UploadResponse.builder().mediaId(UUID.randomUUID()).publicId("existing").build();
        when(persistence.existing(owner, null, memory)).thenReturn(Map.of(hash, existing));
        when(persistence.attach(eq(owner), isNull(), eq(memory), anyList())).thenReturn(List.of(existing));
        assertThat(service.uploadImagesAtomic(List.of(file, file), owner, null, memory)).hasSize(1);
        verifyNoInteractions(storage, cleanup);
    }

    static MockMultipartFile png(int color) throws Exception {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB); image.setRGB(0, 0, color);
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image, "png", bytes);
        return new MockMultipartFile("files", "photo.png", "image/png", bytes.toByteArray());
    }
}
