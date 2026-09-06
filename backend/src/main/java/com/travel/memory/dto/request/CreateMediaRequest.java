package com.travel.memory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMediaRequest {

    @NotBlank(message = "Media url is required")
    @Size(max = 2048, message = "url must not exceed 2048 characters")
    private String url;

    @Size(max = 50, message = "mimeType must not exceed 50 characters")
    private String mimeType;

    private Long fileSize;

    private Integer width;

    private Integer height;

    private Integer displayOrder;
}
