package com.travel.media.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.util.ImageUploadPolicy;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ImageUploadPolicyTest {

    byte[] image(String format) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }

    @Test
    void acceptsRealImagesAndHashesTheActualContent() throws Exception {
        for (String format : new String[] { "jpeg", "png", "gif" }) {
            byte[] bytes = image(format);
            var file = new MockMultipartFile("file", "image." + format, "image/" + format, bytes);
            var inspected = ImageUploadPolicy.inspect(file);
            assertThat(inspected.width()).isEqualTo(8);
            assertThat(inspected.height()).isEqualTo(6);
            assertThat(inspected.hash()).hasSize(64).isEqualTo(ImageUploadPolicy.inspect(file).hash());
            assertThat(inspected.mimeType()).isEqualTo("image/" + format);
        }
    }

    @Test
    void rejectsSpoofedMimeAndNonImageBytes() throws Exception {
        assertThatThrownBy(() ->
            ImageUploadPolicy.inspect(new MockMultipartFile("file", "image.png", "image/png", image("jpeg")))
        ).isInstanceOfSatisfying(AppException.class, error ->
            assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_FILE_TYPE)
        );
        assertThatThrownBy(() ->
            ImageUploadPolicy.inspect(
                new MockMultipartFile("file", "image.jpg", "image/jpeg", "not an image".getBytes())
            )
        ).isInstanceOfSatisfying(AppException.class, error ->
            assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_FILE_TYPE)
        );
    }

    @Test
    void rejectsDecompressionBombDimensionsBeforeDecoding() {
        byte[] header = new byte[30];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, header, 0, 4);
        System.arraycopy("WEBPVP8X".getBytes(StandardCharsets.US_ASCII), 0, header, 8, 8);
        header[24] = (byte) 0xff;
        header[25] = (byte) 0xff;
        header[27] = (byte) 0xff;
        header[28] = (byte) 0xff;
        assertThatThrownBy(() ->
            ImageUploadPolicy.inspect(new MockMultipartFile("file", "large.webp", "image/webp", header))
        ).isInstanceOfSatisfying(AppException.class, error ->
            assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_IMAGE_TOO_LARGE)
        );
    }

    @Test
    void rejectsOversizedFilesBeforeReadingBytes() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(ImageUploadPolicy.MAX_FILE_BYTES + 1);
        assertThatThrownBy(() -> ImageUploadPolicy.inspect(file)).isInstanceOfSatisfying(AppException.class, error ->
            assertThat(error.getErrorCode()).isEqualTo(ErrorCode.UPLOAD_FILE_TOO_LARGE)
        );
        verify(file, never()).getBytes();
    }
}
