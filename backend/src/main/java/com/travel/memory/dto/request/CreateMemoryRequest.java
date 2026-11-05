package com.travel.memory.dto.request;

import com.travel.media.dto.CreateMediaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemoryRequest {
    private UUID clientRequestId;

    private UUID tripId;

    private UUID placeId;

    @Size(max = 2000, message = "note must not exceed 2000 characters")
    private String note;

    @PastOrPresent(message = "experiencedAt cannot be in the future")
    private Instant experiencedAt;

    @Valid
    @Builder.Default
    private List<CreateMediaRequest> media = new ArrayList<>();
}
