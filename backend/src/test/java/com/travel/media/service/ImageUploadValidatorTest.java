package com.travel.media.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.config.MediaProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

class ImageUploadValidatorTest {
    MediaProperties limits = new MediaProperties();
    ImageUploadValidator validator = new ImageUploadValidator(limits);

    @Test void rejectsMimeSpoofingAndDimensionsBeforeDecoding() throws Exception {
        var file = AtomicMediaServiceTest.png(1);
        assertThatThrownBy(() -> validator.read(new MockMultipartFile("files", "x.jpg", "image/jpeg", file.getBytes())))
                .isInstanceOf(AppException.class).extracting(e -> ((AppException)e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_INVALID_FILE_TYPE);
        limits.setMaxPixels(3);
        assertThatThrownBy(() -> validator.read(file)).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException)e).getErrorCode()).isEqualTo(ErrorCode.UPLOAD_IMAGE_TOO_LARGE);
    }

    @Test void sizeLimitAllowsBoundaryAndRejectsOneByteMore() throws Exception {
        var file = AtomicMediaServiceTest.png(1);
        limits.setMaxFileBytes(file.getSize());
        validator.validateBatch(java.util.List.of(file));
        limits.setMaxFileBytes(file.getSize() - 1);
        assertThatThrownBy(() -> validator.validateBatch(java.util.List.of(file))).isInstanceOf(AppException.class);
    }
}
