package com.travel.media.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.config.MediaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ImageUploadValidator {
    private static final Map<String, String> TYPES = Map.of(
            "jpeg", "image/jpeg", "jpg", "image/jpeg", "png", "image/png", "gif", "image/gif", "webp", "image/webp");
    private final MediaProperties limits;

    public record Image(byte[] bytes, String mimeType, int width, int height, String hash) { }

    public void validateBatch(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) throw new AppException(ErrorCode.UPLOAD_FILE_EMPTY);
        if (files.size() > limits.getMaxFilesPerRequest()) throw new AppException(ErrorCode.UPLOAD_TOO_MANY_FILES);
        long total = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) throw new AppException(ErrorCode.UPLOAD_FILE_EMPTY);
            if (file.getSize() > limits.getMaxFileBytes()) throw new AppException(ErrorCode.UPLOAD_FILE_TOO_LARGE);
            total += file.getSize();
            if (total > limits.getMaxBatchBytes()) throw new AppException(ErrorCode.UPLOAD_BATCH_TOO_LARGE);
        }
    }

    public Image read(MultipartFile file) {
        String declared = file.getContentType();
        if (declared == null || !TYPES.containsValue(declared.toLowerCase(Locale.ROOT))) invalid();
        try {
            byte[] bytes = file.getBytes();
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) return invalid();
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    String detected = TYPES.get(reader.getFormatName().toLowerCase(Locale.ROOT));
                    if (detected == null || !detected.equalsIgnoreCase(declared)) return invalid();
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > limits.getMaxDimension() || height > limits.getMaxDimension()
                            || (long) width * height > limits.getMaxPixels()) throw new AppException(ErrorCode.UPLOAD_IMAGE_TOO_LARGE);
                    // Read pixels only after dimensions are bounded; a plausible header is not enough.
                    var image = reader.read(0);
                    if (image == null) return invalid();
                    image.flush();
                    return new Image(bytes, detected, width, height,
                            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof AppException app) throw app;
            return invalid();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Image invalid() { throw new AppException(ErrorCode.UPLOAD_INVALID_FILE_TYPE); }
}
