package com.travel.memory.service;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.CreateMediaRequest;
import com.travel.media.entity.Media;
import com.travel.memory.dto.request.CreateMemoryRequest;
import com.travel.memory.dto.response.MemoryResponse;
import com.travel.memory.entity.Memory;
import com.travel.memory.mapper.MemoryMapper;
import com.travel.memory.repository.MemoryRepository;
import com.travel.memory.service.impl.MemoryServiceImpl;
import com.travel.trip.entity.Trip;
import com.travel.trip.repository.TripRepository;
import com.travel.trip.repository.TripPlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private MemoryRepository memoryRepository;

    @Mock private TripRepository tripRepository;
    @Mock private TripPlaceRepository tripPlaceRepository;

    @Spy
    private MemoryMapper memoryMapper = new MemoryMapper();

    @InjectMocks
    private MemoryServiceImpl memoryService;

    private UUID userId;
    private UUID tripId;
    private UUID placeId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        placeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Create memory with media list succeeds")
    void createMemory_WithMedia_Success() {
        CreateMediaRequest mediaReq = CreateMediaRequest.builder()
                .url("https://cdn.travel.com/photos/1.jpg")
                .mimeType("image/jpeg")
                .fileSize(1024L)
                .width(1920)
                .height(1080)
                .displayOrder(0)
                .build();

        CreateMemoryRequest request = CreateMemoryRequest.builder()
                .tripId(tripId)
                .placeId(placeId)
                .note("Great memory in Da Nang")
                .experiencedAt(Instant.now())
                .media(List.of(mediaReq))
                .build();

        when(memoryRepository.save(any(Memory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tripRepository.findByIdAndUserIdAndDeletedAtIsNull(tripId, userId)).thenReturn(Optional.of(new Trip()));
        when(tripPlaceRepository.existsByTripIdAndPlaceId(tripId, placeId)).thenReturn(true);

        MemoryResponse response = memoryService.createMemory(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTripId()).isEqualTo(tripId);
        assertThat(response.getNote()).isEqualTo("Great memory in Da Nang");
        assertThat(response.getMediaCount()).isEqualTo(1);
        assertThat(response.getCoverImageUrl()).isEqualTo("https://cdn.travel.com/photos/1.jpg");
        assertThat(response.getMedia()).hasSize(1);
        assertThat(response.getMedia().getFirst().getUrl()).isEqualTo("https://cdn.travel.com/photos/1.jpg");

        verify(memoryRepository, times(1)).save(any(Memory.class));
    }

    @Test
    @DisplayName("Get memory by ID succeeds when owned by user")
    void getMemoryById_Success() {
        Memory memory = Memory.create(userId, tripId, placeId, "Sunset at beach", Instant.now());
        Media media = Media.create("https://cdn.travel.com/beach.jpg", "image/jpeg", 2048L, 800, 600, 0);
        memory.addMedia(media);

        UUID memoryId = UUID.randomUUID();
        when(memoryRepository.findByIdAndDeletedAtIsNull(memoryId)).thenReturn(Optional.of(memory));

        MemoryResponse response = memoryService.getMemoryById(memoryId, userId);

        assertThat(response).isNotNull();
        assertThat(response.getNote()).isEqualTo("Sunset at beach");
        assertThat(response.getCoverImageUrl()).isEqualTo("https://cdn.travel.com/beach.jpg");
        assertThat(response.getMediaCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Get memory by ID throws exception when accessed by different user")
    void getMemoryById_AccessDenied() {
        Memory memory = Memory.create(userId, tripId, placeId, "Private note", Instant.now());
        UUID memoryId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        when(memoryRepository.findByIdAndDeletedAtIsNull(memoryId)).thenReturn(Optional.of(memory));

        assertThatThrownBy(() -> memoryService.getMemoryById(memoryId, otherUserId))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.MEMORY_ACCESS_DENIED));
    }

    @Test
    @DisplayName("Get memories by trip returns paginated list")
    void getMemoriesByTrip_Success() {
        Memory memory1 = Memory.create(userId, tripId, placeId, "Day 1", Instant.now());
        Memory memory2 = Memory.create(userId, tripId, placeId, "Day 2", Instant.now());
        Page<Memory> page = new PageImpl<>(List.of(memory1, memory2));

        when(memoryRepository.findByTripIdAndUserIdAndDeletedAtIsNull(eq(tripId), eq(userId), any())).thenReturn(page);

        Page<MemoryResponse> result = memoryService.getMemoriesByTrip(tripId, userId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getNote()).isEqualTo("Day 1");
    }

    @Test
    @DisplayName("Soft delete memory marks deletedAt")
    void deleteMemory_Success() {
        Memory memory = Memory.create(userId, tripId, placeId, "To be deleted", Instant.now());
        UUID memoryId = UUID.randomUUID();

        when(memoryRepository.findByIdAndDeletedAtIsNull(memoryId)).thenReturn(Optional.of(memory));

        memoryService.deleteMemory(memoryId, userId);

        assertThat(memory.isDeleted()).isTrue();
        verify(memoryRepository, times(1)).save(memory);
    }
}
