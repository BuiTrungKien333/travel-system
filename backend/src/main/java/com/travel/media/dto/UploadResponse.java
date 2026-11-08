package com.travel.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    private UUID mediaId;
    private String url;
    private String publicId;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
}
