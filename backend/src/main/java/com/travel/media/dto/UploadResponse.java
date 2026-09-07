package com.travel.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    private String url;
    private String publicId;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
}
