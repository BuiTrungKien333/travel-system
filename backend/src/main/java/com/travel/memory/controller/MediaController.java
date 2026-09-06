package com.travel.memory.controller;


import com.travel.memory.service.MemoryService;
import com.travel.security.model.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal/media")
@RequiredArgsConstructor
@Slf4j(topic = "MEDIA_CONTROLLER")
@Tag(name = "Journal Media Management", description = "Endpoints for managing media items attached to memories")
public class MediaController {

    private final MemoryService memoryService;

    /**
     * Delete a specific media item by ID.
     * DELETE /api/v1/journal/media?id=... (alias: /delete-media)
     */
    @DeleteMapping(value = {"", "/delete-media"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete media item", description = "Permanently deletes a specific media item from a memory. Requires ownership.")
    public void deleteMedia(
            @RequestParam("id") UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UUID currentUserId = currentUser.getId();
        log.info("User [{}] is deleting media [{}]", currentUserId, id);
        memoryService.deleteMedia(id, currentUserId);
    }
}
