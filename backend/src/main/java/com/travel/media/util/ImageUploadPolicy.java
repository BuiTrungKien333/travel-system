package com.travel.media.util;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Reads dimensions from the image header, without allocating a decompressed bitmap.
 */
public final class ImageUploadPolicy {

    public static final long MAX_FILE_BYTES = 10 * 1024 * 1024;
    public static final int MAX_PIXELS = 25_000_000;
    public static final int MAX_DIMENSION = 12_000;
    private static final Map<String, String> MIME = Map.of(
        "JPEG",
        "image/jpeg",
        "PNG",
        "image/png",
        "GIF",
        "image/gif"
    );

    private ImageUploadPolicy() {}

    public record Limits(long maxFileBytes, int maxPixels, int maxDimension) {}

    public static Limits limits() {
        return new Limits(MAX_FILE_BYTES, MAX_PIXELS, MAX_DIMENSION);
    }

    public record Image(byte[] bytes, String mimeType, int width, int height, String hash) {}

    public static Image inspect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.UPLOAD_FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new AppException(ErrorCode.UPLOAD_FILE_TOO_LARGE);
        }

        String declared = file.getContentType();
        if (
            declared == null ||
            !java.util.Set.of("image/jpeg", "image/png", "image/gif", "image/webp").contains(
                declared.toLowerCase(Locale.ROOT)
            )
        ) {
            throw new AppException(ErrorCode.UPLOAD_INVALID_FILE_TYPE);
        }

        try {
            byte[] bytes = file.getBytes();
            int width;
            int height;
            String actual;

            if (bytes.length >= 30 && text(bytes, 0, 4).equals("RIFF") && text(bytes, 8, 4).equals("WEBP")) {
                actual = "image/webp";
                String chunk = text(bytes, 12, 4);
                if (chunk.equals("VP8X")) {
                    width = 1 + little24(bytes, 24);
                    height = 1 + little24(bytes, 27);
                } else if (
                    chunk.equals("VP8 ") &&
                    (bytes[23] & 255) == 0x9d &&
                    (bytes[24] & 255) == 1 &&
                    (bytes[25] & 255) == 0x2a
                ) {
                    width = ((bytes[26] & 255) | ((bytes[27] & 255) << 8)) & 0x3fff;
                    height = ((bytes[28] & 255) | ((bytes[29] & 255) << 8)) & 0x3fff;
                } else if (chunk.equals("VP8L") && (bytes[20] & 255) == 0x2f) {
                    width = 1 + ((bytes[21] & 255) | ((bytes[22] & 0x3f) << 8));
                    height = 1 + (((bytes[22] & 0xc0) >> 6) | ((bytes[23] & 255) << 2) | ((bytes[24] & 15) << 10));
                } else {
                    throw new AppException(ErrorCode.UPLOAD_INVALID_FILE_TYPE);
                }
            } else {
                try (
                    var input = new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))
                ) {
                    var readers = ImageIO.getImageReaders(input);
                    if (!readers.hasNext()) {
                        throw new AppException(ErrorCode.UPLOAD_INVALID_FILE_TYPE);
                    }
                    var reader = readers.next();
                    try {
                        reader.setInput(input, true, true);
                        actual = MIME.get(reader.getFormatName().toUpperCase(Locale.ROOT));
                        width = reader.getWidth(0);
                        height = reader.getHeight(0);
                    } finally {
                        reader.dispose();
                    }
                }
            }

            if (!declared.equalsIgnoreCase(actual) || width <= 0 || height <= 0) {
                throw new AppException(ErrorCode.UPLOAD_INVALID_FILE_TYPE);
            }
            if (width > MAX_DIMENSION || height > MAX_DIMENSION || (long) width * height > MAX_PIXELS) {
                throw new AppException(ErrorCode.UPLOAD_IMAGE_TOO_LARGE);
            }

            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return new Image(bytes, actual, width, height, hash);
        } catch (IOException error) {
            throw new AppException(ErrorCode.UPLOAD_INVALID_FILE_TYPE);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String text(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    private static int little24(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | ((bytes[offset + 1] & 255) << 8) | ((bytes[offset + 2] & 255) << 16);
    }
}
