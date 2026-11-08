package com.travel.media.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {
    @Min(1024) @Max(10485760) private long maxFileBytes = 10 * 1024 * 1024;
    @Min(1024) @Max(104857600) private long maxBatchBytes = 50 * 1024 * 1024;
    @Min(1) @Max(10) private int maxFilesPerRequest = 10;
    @Min(1) @Max(100) private int maxFilesPerMemory = 20;
    @Min(1) @Max(40000000) private long maxPixels = 25_000_000;
    @Min(1) @Max(12000) private int maxDimension = 12_000;
    @Min(1) @Max(1000) private int uploadsPerMinute = 20;
    @Min(1) @Max(10) private int concurrentUploadsPerUser = 2;
    @Min(1) @Max(32) private int concurrentUploads = 2;
}
