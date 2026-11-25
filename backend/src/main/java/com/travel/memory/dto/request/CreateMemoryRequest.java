package com.travel.memory.dto.request;

import com.travel.media.dto.CreateMediaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
public class CreateMemoryRequest {

    private UUID clientRequestId;

    private UUID tripId;

    private UUID placeId;

    @Size(max = 2000, message = "note must not exceed 2000 characters")
    private String note;

    // Capture time comes from the device; clock skew must not block photo synchronization.
    private Instant experiencedAt;

    @Valid
    @Size(max = 0, message = "Upload image files through the media endpoint")
    @Builder.Default
    private List<CreateMediaRequest> media = new ArrayList<>();
}
