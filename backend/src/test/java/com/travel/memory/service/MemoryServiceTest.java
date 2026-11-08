package com.travel.memory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.CreateMediaRequest;
import com.travel.media.entity.Media;
import com.travel.media.service.MediaCleanupQueue;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.request.UpdateMemoryRequest;
import com.travel.memory.entity.Memory;
import com.travel.memory.mapper.MemoryMapper;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.impl.MemoryServiceImpl;
import com.travel.trip.entity.Trip;
import com.travel.trip.repository.TripRepository;
import com.travel.trip.repository.TripPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {
    @Mock MemoryRepository memories;
    @Mock TripRepository trips;
    @Mock TripPlaceRepository places;
    @Mock MediaCleanupQueue cleanup;
    MemoryServiceImpl service;
    UUID owner = UUID.randomUUID(), trip = UUID.randomUUID(), place = UUID.randomUUID(), id = UUID.randomUUID();
    Clock clock = Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach void setup() {
        service = new MemoryServiceImpl(memories, new MemoryMapper(), trips, places,
                new MemoryAccess(memories, trips), cleanup, new ObjectMapper().findAndRegisterModules(), clock);
    }

    @Test void createsTrimmedNoteWithOwnedTripAndPlace() {
        when(trips.lockOwnedTrip(trip, owner)).thenReturn(Optional.of(new Trip()));
        when(places.existsByTripIdAndPlaceId(trip, place)).thenReturn(true);
        when(memories.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var response = service.createMemory(owner, CreateMemoryRequest.builder().tripId(trip).placeId(place).note("  A sunset  ").build());
        assertThat(response.getUserId()).isEqualTo(owner);
        assertThat(response.getTripId()).isEqualTo(trip);
        assertThat(response.getNote()).isEqualTo("A sunset");
        assertThat(response.getMedia()).isEmpty();
    }

    @Test void arbitraryMediaUrlsCannotBypassVerifiedUploads() {
        var request = CreateMemoryRequest.builder().media(List.of(CreateMediaRequest.builder().url("https://untrusted.example/photo").build())).build();
        assertThatThrownBy(() -> service.createMemory(owner, request)).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        verifyNoInteractions(memories);
    }

    @Test void detailsIncludeAllPhotosForOwner() {
        var memory = Memory.create(owner, null, null, "Sunset", clock.instant());
        memory.addMedia(Media.create("https://example.com/photo.jpg", "image/jpeg", 200L, 10, 10, 0));
        when(memories.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(memory));
        assertThat(service.getMemoryById(id, owner).getMedia()).hasSize(1);
    }

    @Test void detailsRejectOtherOwner() {
        when(memories.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(Memory.create(owner, null, null, "Private", clock.instant())));
        assertThatThrownBy(() -> service.getMemoryById(id, UUID.randomUUID())).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.MEMORY_ACCESS_DENIED);
    }

    @Test void paginationIsBoundedAndTripMustBeOwned() {
        when(trips.findByIdAndUserIdAndDeletedAtIsNull(trip, owner)).thenReturn(Optional.of(new Trip()));
        when(memories.ownerIds(eq(owner), eq(trip), any())).thenReturn(new PageImpl<>(List.of()));
        assertThat(service.getMemoriesByTrip(trip, owner, PageRequest.of(0, 50))).isEmpty();
        assertThat(service.getMemoriesByTrip(trip, owner, PageRequest.of(0, 51)).getSize()).isEqualTo(50);
    }

    @Test void deleteSoftDeletesAndQueuesStorageCleanup() {
        var memory = lockedMemory();
        var photo = Media.create("https://example.com/photo.jpg", "image/jpeg", 200L, 10, 10, 0);
        photo.setPublicId("owned/photo"); memory.addMedia(photo);
        service.deleteMemory(id, owner);
        assertThat(memory.isDeleted()).isTrue();
        verify(cleanup).enqueue("owned/photo");
    }

    @Test void staleVersionCannotOverwriteNote() {
        var memory = lockedMemory();
        var request = new UpdateMemoryRequest(); request.setVersion(1); request.setNote("Overwrite");
        assertThatThrownBy(() -> service.updateMemory(id, owner, request)).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.MEMORY_VERSION_CONFLICT);
        assertThat(memory.getNote()).isEqualTo("Original");
    }

    Memory lockedMemory() {
        var memory = Memory.create(owner, null, null, "Original", clock.instant());
        var ref = mock(MemoryRepository.Reference.class);
        when(ref.getUserId()).thenReturn(owner);
        when(memories.reference(id)).thenReturn(Optional.of(ref));
        when(memories.lockActive(id, owner)).thenReturn(Optional.of(memory));
        return memory;
    }
}
