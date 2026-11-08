package com.travel.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.media.dto.UploadResponse;
import com.travel.media.service.CloudinaryStorage;
import com.travel.media.service.MediaCleanupQueue;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.media.max-files-per-memory=2", "app.media.uploads-per-minute=1000"})
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MemoryMediaIntegrationTest {
    static final String BASE = "/api/v1/journal/memories";
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired MediaCleanupQueue cleanup;
    @MockitoBean CloudinaryStorage storage;
    @MockitoBean com.travel.media.service.CloudinaryImageDeletion remoteDeletion;
    @MockitoBean AuthEmailSender email;
    MockMvc mvc;
    UUID owner, trip;
    String token;

    @BeforeEach void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.execute("TRUNCATE media_cleanup_jobs, memories CASCADE");
        String password = UUID.randomUUID().toString();
        var user = users.saveAndFlush(User.builder().fullName("Memory Owner").email(UUID.randomUUID()+"@example.com")
                .passwordHash(passwords.encode(password)).emailVerifiedAt(Instant.now()).build());
        owner = user.getId(); trip = UUID.randomUUID();
        token = data(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("email", user.getEmail(), "password", password)))).andExpect(status().isOk())).path("accessToken").asText();
        jdbc.update("INSERT INTO trips(id,user_id,name,destination,start_date,end_date) VALUES (?,?,'Photo trip','Da Nang','2027-01-01','2027-01-03')", trip, owner);
        when(storage.upload(any(), anyString())).thenAnswer(i -> {
            var image = (com.travel.media.service.ImageUploadValidator.Image)i.getArgument(0);
            return UploadResponse.builder().publicId(i.getArgument(1)).url("https://res.cloudinary.com/test/image/upload/"+i.getArgument(1)+".png")
                    .mimeType(image.mimeType()).fileSize((long)image.bytes().length).width(image.width()).height(image.height()).build();
        });
    }

    @Test void uploadPersistsVerifiedMetadataAndAllPhotosAreReadable() throws Exception {
        String id = create().path("id").asText();
        upload(id, png(1), png(2)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get(BASE+"/"+id).header("Authorization", "Bearer "+token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mediaCount").value(2)).andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.media[0].width").value(2)).andExpect(jsonPath("$.data.media[1].id").isNotEmpty());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media WHERE public_id IS NOT NULL AND length(upload_hash)=64", Integer.class)).isEqualTo(2);
        assertThat(jobs()).isZero();
    }

    @Test void fakeImageAndMissingMemoryNeverReachProvider() throws Exception {
        String id = create().path("id").asText();
        upload(id, new MockMultipartFile("files", "fake.png", "image/png", "text pretending to be an image".getBytes()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UPLOAD_003"));
        mvc.perform(multipart("/api/v1/media/uploads/atomic").file(png(1)).header("Authorization", "Bearer "+token))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/media/uploads/atomic").file(png(1)).param("memoryId", id)).andExpect(status().isUnauthorized());
        verify(storage, never()).upload(any(), anyString());
    }

    @Test void foreignMemoryAndTripCannotReceiveUploads() throws Exception {
        String id = create().path("id").asText();
        mvc.perform(multipart("/api/v1/media/uploads/atomic").file(png(1)).param("memoryId", id).param("tripId", UUID.randomUUID().toString())
                .header("Authorization", "Bearer "+token)).andExpect(status().isForbidden());
        UUID other = users.saveAndFlush(User.builder().fullName("Other").email(UUID.randomUUID()+"@example.com").build()).getId();
        jdbc.update("UPDATE memories SET user_id=? WHERE id=?", other, UUID.fromString(id));
        upload(id, png(1)).andExpect(status().isForbidden());
        verify(storage, never()).upload(any(), anyString());
    }

    @Test void retriesAndDuplicateFilesReuseTheSameMedia() throws Exception {
        String id = create().path("id").asText();
        var first = data(upload(id, png(1), png(1)).andExpect(status().isCreated()));
        var again = data(upload(id, png(1)).andExpect(status().isCreated()));
        assertThat(first).hasSize(1);
        assertThat(again.get(0).path("mediaId")).isEqualTo(first.get(0).path("mediaId"));
        assertThat(countMedia(id)).isEqualTo(1);
        verify(storage, times(1)).upload(any(), anyString());
    }

    @Test void fullMemoryRejectsNewPhotoBeforeCallingStorage() throws Exception {
        String id = create().path("id").asText();
        upload(id, png(1), png(2)).andExpect(status().isCreated());
        upload(id, png(3)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("UPLOAD_007"));
        assertThat(countMedia(id)).isEqualTo(2);
        assertThat(jobs()).isZero();
        verify(storage, times(2)).upload(any(), anyString());
    }

    @Test void partialProviderFailureLeavesNoPartialMemoryBatch() throws Exception {
        String id = create().path("id").asText();
        var call = new AtomicInteger();
        doAnswer(i -> {
            if (call.incrementAndGet() == 2) throw new AppException(ErrorCode.UPLOAD_FAILED);
            return UploadResponse.builder().publicId(i.getArgument(1)).url("https://example.com/photo.png").build();
        }).when(storage).upload(any(), anyString());
        upload(id, png(1), png(2)).andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Không thể tải ảnh lên kho lưu trữ. Vui lòng thử lại."));
        assertThat(countMedia(id)).isZero(); assertThat(jobs()).isEqualTo(2);
        cleanup.drain(); assertThat(jobs()).isZero();
        verify(storage, times(2)).destroy(anyString());
    }

    @Test void databaseFailureRetainsDurableCleanupIntent() throws Exception {
        String id = create().path("id").asText();
        jdbc.execute("ALTER TABLE media ADD CONSTRAINT memory_test_write_failure CHECK (memory_id <> '"+id+"'::uuid)");
        try {
            upload(id, png(1)).andExpect(status().is5xxServerError());
            assertThat(countMedia(id)).isZero(); assertThat(jobs()).isEqualTo(1);
        } finally { jdbc.execute("ALTER TABLE media DROP CONSTRAINT memory_test_write_failure"); }
    }

    @Test void concurrentDistinctUploadsCannotExceedQuota() throws Exception {
        String id = create().path("id").asText(); upload(id, png(1)).andExpect(status().isCreated());
        synchronizeProvider();
        assertThat(race(() -> upload(id, png(2)).andReturn().getResponse().getStatus(),
                () -> upload(id, png(3)).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(201, 409);
        assertThat(countMedia(id)).isEqualTo(2); assertThat(jobs()).isEqualTo(1);
    }

    @Test void concurrentIdenticalUploadsCreateOneMedia() throws Exception {
        String id = create().path("id").asText(); synchronizeProvider();
        assertThat(race(() -> upload(id, png(1)).andReturn().getResponse().getStatus(),
                () -> upload(id, png(1)).andReturn().getResponse().getStatus())).containsOnly(201);
        assertThat(countMedia(id)).isEqualTo(1); assertThat(jobs()).isEqualTo(1);
    }

    @Test void editChecksVersionAndReorderSetsCoverWithoutChangingMembership() throws Exception {
        String id = create().path("id").asText();
        var photos = data(upload(id, png(1), png(2)).andExpect(status().isCreated()));
        var ids = List.of(photos.get(1).path("mediaId").asText(), photos.get(0).path("mediaId").asText());
        var body = new LinkedHashMap<String,Object>(); body.put("version", 1); body.put("note", null); body.put("mediaOrder", ids);
        var updated = data(patchMemory(id, body).andExpect(status().isOk()));
        assertThat(updated.path("version").asInt()).isEqualTo(2);
        assertThat(updated.path("coverImageUrl")).isEqualTo(photos.get(1).path("url"));
        assertThat(updated.path("note").isMissingNode() || updated.path("note").isNull()).isTrue();
        patchMemory(id, Map.of("version", 1, "note", "Stale")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMORY_003"));
        patchMemory(id, Map.of("version", 2, "mediaOrder", List.of(ids.get(0), ids.get(0)))).andExpect(status().isBadRequest());
        patchMemory(id, Map.of("version", 2.5, "note", "Bad")).andExpect(status().isBadRequest());
        patchMemory(id, Map.of("version", 2, "noteProvided", true)).andExpect(status().isBadRequest());
        assertThat(countMedia(id)).isEqualTo(2);
    }

    @Test void idempotentCreateAndPayloadConflictDoNotDuplicateMemory() throws Exception {
        var body = new LinkedHashMap<String,Object>(Map.of("tripId", trip, "note", "A note", "clientRequestId", UUID.randomUUID()));
        String first = data(postMemory(body).andExpect(status().isCreated())).path("id").asText();
        assertThat(data(postMemory(body).andExpect(status().isCreated())).path("id").asText()).isEqualTo(first);
        body.put("note", "Different");
        postMemory(body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMORY_004"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM memories", Integer.class)).isEqualTo(1);
    }

    @Test void deletePhotoAndMemoryUseRetryableCleanup() throws Exception {
        String id = create().path("id").asText();
        var photo = data(upload(id, png(1)).andExpect(status().isCreated())).get(0);
        mvc.perform(delete("/api/v1/media").param("id", photo.path("mediaId").asText()).header("Authorization", "Bearer "+token))
                .andExpect(status().isNoContent());
        assertThat(countMedia(id)).isZero(); assertThat(jobs()).isZero();
        verify(remoteDeletion).destroy(photo.path("publicId").asText());
        upload(id, png(2)).andExpect(status().isCreated());
        mvc.perform(delete(BASE+"/"+id).header("Authorization", "Bearer "+token)).andExpect(status().isNoContent());
        doThrow(new IOException("simulated storage outage")).when(storage).destroy(anyString());
        cleanup.drain(); assertThat(jobs()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT attempts FROM media_cleanup_jobs", Integer.class)).isEqualTo(1);
        doNothing().when(storage).destroy(anyString());
        jdbc.update("UPDATE media_cleanup_jobs SET run_after=now()"); cleanup.drain(); assertThat(jobs()).isZero();
        mvc.perform(get(BASE+"/"+id).header("Authorization", "Bearer "+token)).andExpect(status().isNotFound());
    }

    @Test void paginationAndCreateValidationHaveServerLimits() throws Exception {
        create(); create();
        mvc.perform(get(BASE).param("size", "1").header("Authorization", "Bearer "+token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1)).andExpect(jsonPath("$.data.totalElements").value(2));
        mvc.perform(get(BASE).param("size", "51").header("Authorization", "Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(50));
        postMemory(Map.of("media", List.of(Map.of("url", "https://untrusted.example/photo")))).andExpect(status().isBadRequest());
        postMemory(Map.of("note", "a".repeat(2001))).andExpect(status().isBadRequest());
        postMemory(Map.of("experiencedAt", "2100-01-01T00:00:00Z")).andExpect(status().isBadRequest());
    }

    @Test void tripDeletionAlsoCleansUpStorageAssets() throws Exception {
        String id = create().path("id").asText(); upload(id, png(1)).andExpect(status().isCreated());
        mvc.perform(delete("/api/v1/trips/"+trip).param("version", "0").header("Authorization", "Bearer "+token)).andExpect(status().isNoContent());
        cleanup.drain();
        assertThat(jobs()).isZero();
        assertThat(jdbc.queryForObject("SELECT storage_deleted_at IS NOT NULL FROM media WHERE memory_id=?", Boolean.class, UUID.fromString(id))).isTrue();
    }

    @Test void cleanupNeverDeletesAnAssetAttachedToAnActiveMemory() throws Exception {
        String id = create().path("id").asText();
        var photo = data(upload(id, png(1)).andExpect(status().isCreated())).get(0);
        cleanup.enqueue(photo.path("publicId").asText());
        cleanup.drain();
        verify(storage, never()).destroy(anyString());
        assertThat(countMedia(id)).isEqualTo(1); assertThat(jobs()).isZero();
    }

    @Test void rateLimitReturnsRetryAfterAndDoesNotCallStorageAgain() throws Exception {
        String id = create().path("id").asText();
        var limits = context.getBean(com.travel.media.config.MediaProperties.class);
        limits.setUploadsPerMinute(1);
        try {
            upload(id, png(1)).andExpect(status().isCreated());
            upload(id, png(2)).andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
            verify(storage, times(1)).upload(any(), anyString());
        } finally { limits.setUploadsPerMinute(1000); }
    }

    JsonNode create() throws Exception { return data(postMemory(Map.of("tripId", trip, "note", "Original")).andExpect(status().isCreated())); }
    ResultActions postMemory(Object body) throws Exception { return mvc.perform(post(BASE).header("Authorization", "Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))); }
    ResultActions patchMemory(String id, Object body) throws Exception { return mvc.perform(patch(BASE+"/"+id).header("Authorization", "Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body))); }
    JsonNode data(ResultActions result) throws Exception { return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data"); }
    ResultActions upload(String id, MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/media/uploads/atomic");
        for (var file : files) request.file(file);
        return mvc.perform(request.param("memoryId", id).param("tripId", trip.toString())
                .header("Authorization", "Bearer "+token).header("Accept-Language", "vi"));
    }
    int countMedia(String id) { return jdbc.queryForObject("SELECT count(*) FROM media WHERE memory_id=?", Integer.class, UUID.fromString(id)); }
    int jobs() { return jdbc.queryForObject("SELECT count(*) FROM media_cleanup_jobs", Integer.class); }
    static MockMultipartFile png(int color) throws Exception {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB); image.setRGB(0, 0, color);
        var bytes = new ByteArrayOutputStream(); ImageIO.write(image, "png", bytes);
        return new MockMultipartFile("files", "image.png", "image/png", bytes.toByteArray());
    }
    void synchronizeProvider() {
        var ready = new CountDownLatch(2);
        doAnswer(i -> {
            ready.countDown(); assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            return UploadResponse.builder().publicId(i.getArgument(1)).url("https://example.com/"+i.getArgument(1)+".png").mimeType("image/png").build();
        }).when(storage).upload(any(), anyString());
    }
    List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { try { return first.call(); } finally { SecurityContextHolder.clearContext(); } });
            var b = pool.submit(() -> { try { return second.call(); } finally { SecurityContextHolder.clearContext(); } });
            return List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        }
    }
}
