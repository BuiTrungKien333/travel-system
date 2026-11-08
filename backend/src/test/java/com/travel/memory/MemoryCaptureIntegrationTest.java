package com.travel.memory;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MemoryCaptureIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired com.travel.media.service.MediaDeletionService deletion;
    @MockitoBean AuthEmailSender mail;
    @MockitoBean Cloudinary cloudinary;
    Uploader uploader;
    MockMvc mvc;
    UUID owner;
    UUID trip;
    UUID place;
    String token;
    byte[] image;

    @BeforeEach void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String password = UUID.randomUUID()+".Ab1";
        User user = users.save(User.builder().fullName("Memory Tester").email(UUID.randomUUID()+"@example.com")
                .passwordHash(encoder.encode(password)).emailVerifiedAt(Instant.now().minusSeconds(600)).build());
        owner = user.getId();
        token = data(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", password))))
                .andExpect(status().isOk())).path("accessToken").asText();
        trip = UUID.fromString(data(mvc.perform(post("/api/v1/trips").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "clientRequestId", UUID.randomUUID(), "destination", Map.of("name", "Đà Lạt"),
                        "startDate", java.time.LocalDate.now(com.travel.trip.service.TripCalendar.ZONE).plusDays(1).toString(),
                        "endDate", java.time.LocalDate.now(com.travel.trip.service.TripCalendar.ZONE).plusDays(3).toString()))))
                .andExpect(status().isCreated())).path("trip").path("id").asText());
        place = UUID.randomUUID();
        jdbc.update("INSERT INTO places(id,name,latitude,longitude) VALUES (?,'Memory fixture',11.94,108.44)", place);
        jdbc.update("INSERT INTO trip_places(trip_id,place_id) VALUES (?,?)", trip, place);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB), "jpeg", bytes);
        image = bytes.toByteArray();
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            Map<?, ?> params = invocation.getArgument(1);
            assertThat(params.get("overwrite")).isEqualTo(false);
            return Map.of("secure_url", "https://res.cloudinary.com/fixture/image/upload/photo.jpg",
                    "public_id", params.get("folder")+"/"+params.get("public_id"), "bytes", image.length, "width", 8, "height", 6);
        });
    }

    String bearer() { return "Bearer " + token; }
    UUID otherOwner() {
        return users.save(User.builder().fullName("Other Tester").email(UUID.randomUUID()+"@example.com")
                .passwordHash(encoder.encode("Unused.fixture.Ab1")).build()).getId();
    }
    JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }
    Map<String,Object> request() {
        var body = new LinkedHashMap<String,Object>();
        body.put("clientRequestId", UUID.randomUUID()); body.put("tripId", trip); body.put("placeId", place);
        body.put("note", "Buổi sáng ở Đà Lạt"); body.put("experiencedAt", Instant.now().minusSeconds(120).toString());
        return body;
    }
    ResultActions create(Map<String,Object> body) throws Exception {
        return mvc.perform(post("/api/v1/journal/memories").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    UUID create() throws Exception { return UUID.fromString(data(create(request()).andExpect(status().isCreated())).path("id").asText()); }
    ResultActions upload(UUID memory) throws Exception {
        return mvc.perform(multipart("/api/v1/media/upload").file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", image))
                .param("memoryId", memory.toString()).param("tripId", trip.toString()).header("Authorization", bearer()));
    }
    long count(String table, UUID memory) {
        return Objects.requireNonNull(jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE "+(table.equals("media")?"memory_id":"id")+"=?", Long.class, memory));
    }

    @Test void captureUploadReplayAndDayReadUseTheSamePersistedPhoto() throws Exception {
        var body = request();
        body.put("experiencedAt", "2026-10-21T17:30:00Z"); // 00:30 on 22/10 in Vietnam.
        UUID id = UUID.fromString(data(create(body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version").value(0))).path("id").asText());
        create(body).andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(id.toString()));
        var first = data(upload(id).andExpect(status().isCreated()));
        upload(id).andExpect(status().isCreated()).andExpect(jsonPath("$.data.mediaId").value(first.path("mediaId").asText()));
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
        assertThat(count("memories", id)).isEqualTo(1);
        assertThat(count("media", id)).isEqualTo(1);
        mvc.perform(get("/api/v1/journal/memories").param("tripId", trip.toString()).param("date", "2026-10-22")
                .header("Authorization", bearer())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].media[0].id").value(first.path("mediaId").asText()));
        mvc.perform(get("/api/v1/journal/memories").param("tripId", trip.toString()).param("date", "2026-10-21")
                .header("Authorization", bearer())).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test void lostProviderResponseCanRetryWithoutCreatingAnotherAsset() throws Exception {
        UUID id = create();
        var publicIds = new ArrayList<Object>();
        when(uploader.upload(any(byte[].class), anyMap())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            Map<?,?> params = invocation.getArgument(1);
            publicIds.add(params.get("public_id"));
            if (publicIds.size() == 1) throw new IOException("Response lost");
            return Map.of("existing", true, "secure_url", "https://res.cloudinary.com/fixture/photo.jpg", "public_id", "fixture/photo");
        });
        upload(id).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("UPLOAD_004"));
        assertThat(count("media", id)).isZero();
        upload(id).andExpect(status().isCreated());
        assertThat(publicIds).hasSize(2); assertThat(publicIds.get(0)).isEqualTo(publicIds.get(1));
        assertThat(count("media", id)).isEqualTo(1);
    }

    @Test void concurrentCreateRetriesConvergeAndChangedPayloadConflicts() throws Exception {
        var body = request();
        var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<String> operation = () -> { barrier.await(10, TimeUnit.SECONDS); return data(create(body)
                    .andExpect(status().isCreated())).path("id").asText(); };
            var a = pool.submit(operation); var b = pool.submit(operation);
            assertThat(a.get(30, TimeUnit.SECONDS)).isEqualTo(b.get(30, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM memories WHERE user_id=?", Long.class, owner)).isEqualTo(1);
        body.put("note", "Different");
        create(body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMORY_004"));
    }

    @Test void concurrentUploadsConvergeInDatabase() throws Exception {
        UUID id = create(); var barrier = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<String> operation = () -> { barrier.await(10, TimeUnit.SECONDS); return data(upload(id)
                    .andExpect(status().isCreated())).path("mediaId").asText(); };
            var a = pool.submit(operation); var b = pool.submit(operation);
            assertThat(a.get(30, TimeUnit.SECONDS)).isEqualTo(b.get(30, TimeUnit.SECONDS));
        }
        assertThat(count("media", id)).isEqualTo(1);
    }

    @Test void noteEditingChecksVersionAndOwnership() throws Exception {
        UUID id = create();
        mvc.perform(patch("/api/v1/journal/memories/"+id).header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"note\":\"Đã cập nhật\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(patch("/api/v1/journal/memories/"+id).header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"note\":\"Stale\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMORY_003"));
        assertThat(jdbc.queryForObject("SELECT note FROM memories WHERE id=?", String.class, id)).isEqualTo("Đã cập nhật");
        jdbc.update("UPDATE memories SET user_id=? WHERE id=?", otherOwner(), id);
        mvc.perform(patch("/api/v1/journal/memories/"+id).header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":1,\"note\":\"Foreign\"}"))
                .andExpect(status().isForbidden());
        upload(id).andExpect(status().isForbidden());
        verifyNoInteractions(uploader);
    }

    @Test void rejectForeignTripUnknownPlaceFutureTimestampAndClientSuppliedMedia() throws Exception {
        var body = request(); body.put("tripId", UUID.randomUUID());
        create(body).andExpect(status().isNotFound());
        body = request(); body.put("placeId", UUID.randomUUID());
        create(body).andExpect(status().isNotFound());
        body = request(); body.put("experiencedAt", Instant.now().plusSeconds(3600).toString());
        create(body).andExpect(status().isBadRequest());
        body = request(); body.put("media", List.of(Map.of("url", "https://attacker.example/image.jpg")));
        create(body).andExpect(status().isBadRequest());
        verifyNoInteractions(uploader);
    }

    @Test void rejectDeletedMemoryOrTripWithoutUploading() throws Exception {
        UUID id = create();
        jdbc.update("UPDATE memories SET deleted_at=now() WHERE id=?", id);
        upload(id).andExpect(status().isNotFound());
        var body = request();
        jdbc.update("UPDATE trips SET deleted_at=now() WHERE id=?", trip);
        create(body).andExpect(status().isNotFound());
        verifyNoInteractions(uploader);
    }

    @Test void paginatedLibraryIsBoundedAndDoesNotExposeOtherOwners() throws Exception {
        UUID first = create(); UUID second = create();
        upload(first).andExpect(status().isCreated()); upload(second).andExpect(status().isCreated());
        jdbc.update("UPDATE memories SET user_id=? WHERE id=?", otherOwner(), second);
        mvc.perform(get("/api/v1/journal/memories").param("size", "500").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.size").value(50))
                .andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.content[0].id").value(first.toString()));
    }

    @Test void limitsMultipartPairAndActualFileContentAreValidated() throws Exception {
        mvc.perform(get("/api/v1/media/limits")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/media/limits").header("Authorization", bearer())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.maxFileBytes").value(10485760)).andExpect(jsonPath("$.data.maxPixels").value(25000000));
        UUID id = create();
        mvc.perform(multipart("/api/v1/media/upload").file(new MockMultipartFile("file", "test.jpg", "image/jpeg", image))
                .param("memoryId", id.toString()).header("Authorization", bearer())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/media/upload").file(new MockMultipartFile("file", "fake.jpg", "image/jpeg", "Not a photo".getBytes()))
                .param("memoryId", id.toString()).param("tripId", trip.toString()).header("Authorization", bearer()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UPLOAD_003"));
        assertThat(count("media", id)).isZero(); verifyNoInteractions(uploader);
    }

    @Test void batchReportsInvalidFilesAndContinuesWithoutDuplicatingSuccessfulPhotos() throws Exception {
        UUID memory = create();
        var second = Arrays.copyOf(image, image.length + 1);
        var request = multipart("/api/v1/media/uploads")
                .file(new MockMultipartFile("files", "first.jpg", "image/jpeg", image))
                .file(new MockMultipartFile("files", "invalid.jpg", "image/jpeg", "not an image".getBytes()))
                .file(new MockMultipartFile("files", "second.jpg", "image/jpeg", second))
                .param("tripId", trip.toString()).param("memoryId", memory.toString()).header("Authorization", bearer());
        mvc.perform(request).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.succeeded").value(2)).andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.items[0].media.mediaId").exists())
                .andExpect(jsonPath("$.data.items[1].index").value(1))
                .andExpect(jsonPath("$.data.items[1].errorCode").value("UPLOAD_003"))
                .andExpect(jsonPath("$.data.items[2].media.mediaId").exists());
        mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.succeeded").value(2));
        assertThat(count("media", memory)).isEqualTo(2);
        verify(uploader, times(2)).upload(any(byte[].class), anyMap());
    }

    @Test void batchProviderFailureDoesNotPreventOtherItemsAndCanRetry() throws Exception {
        UUID memory = create();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            if (calls.getAndIncrement() == 0) throw new IOException("fixture failure");
            Map<?, ?> params = invocation.getArgument(1);
            return Map.of("secure_url", "https://res.cloudinary.com/fixture/image/upload/photo.jpg",
                    "public_id", params.get("folder") + "/" + params.get("public_id"));
        }).when(uploader).upload(any(byte[].class), anyMap());
        var request = multipart("/api/v1/media/uploads")
                .file(new MockMultipartFile("files", "first.jpg", "image/jpeg", image))
                .file(new MockMultipartFile("files", "second.jpg", "image/jpeg", Arrays.copyOf(image, image.length + 1)))
                .param("tripId", trip.toString()).param("memoryId", memory.toString()).header("Authorization", bearer());
        mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.succeeded").value(1))
                .andExpect(jsonPath("$.data.items[0].errorCode").value("UPLOAD_004"));
        mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.succeeded").value(2));
        assertThat(count("media", memory)).isEqualTo(2);
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test void batchRequiresOwnedMemoryAndEnforcesRequestBoundariesBeforeProviderCalls() throws Exception {
        UUID memory = create();
        var request = multipart("/api/v1/media/uploads").param("tripId", trip.toString())
                .param("memoryId", memory.toString()).header("Authorization", bearer());
        for (int i=0; i<11; i++) request.file(new MockMultipartFile("files", "photo.jpg", "image/jpeg", image));
        mvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("UPLOAD_005"));
        jdbc.update("UPDATE memories SET user_id=? WHERE id=?", otherOwner(), memory);
        mvc.perform(multipart("/api/v1/media/uploads").file(new MockMultipartFile("files", "photo.jpg", "image/jpeg", image))
                .param("tripId", trip.toString()).param("memoryId", memory.toString()).header("Authorization", bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/media/uploads").file(new MockMultipartFile("files", "photo.jpg", "image/jpeg", image))
                .param("tripId", trip.toString()).header("Authorization", bearer())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/media/uploads").file(new MockMultipartFile("files", "photo.jpg", "image/jpeg", image))
                .param("tripId", trip.toString()).param("memoryId", memory.toString())).andExpect(status().isUnauthorized());
        verifyNoInteractions(uploader);
    }

    UUID uploadedPhoto(UUID memory) throws Exception {
        return UUID.fromString(data(upload(memory).andExpect(status().isCreated())).path("mediaId").asText());
    }
    ResultActions erase(UUID media) throws Exception {
        return mvc.perform(delete("/api/v1/media").param("id", media.toString()).header("Authorization", bearer()));
    }
    void storageDeletesSuccessfully() throws Exception {
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            Map<?, ?> parameters = invocation.getArgument(1);
            assertThat(parameters.get("invalidate")).isEqualTo(true);
            assertThat(parameters.get("resource_type")).isEqualTo("image");
            return Map.of("result", "ok");
        }).when(uploader).destroy(anyString(), anyMap());
    }

    @Test void deleteConfirmsCloudAndRepeatsWithoutAnotherProviderCall() throws Exception {
        UUID memory = create(), photo = uploadedPhoto(memory);
        storageDeletesSuccessfully();
        erase(photo).andExpect(status().isNoContent());
        assertThat(count("media", memory)).isZero();
        assertThat(jdbc.queryForObject("SELECT completed_at IS NOT NULL FROM media_deletions WHERE media_id=?", Boolean.class, photo)).isTrue();
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM memories WHERE id=?", Boolean.class, memory)).isTrue();
        erase(photo).andExpect(status().isNoContent());
        verify(uploader, times(1)).destroy(anyString(), anyMap());
        mvc.perform(get("/api/v1/journal/memories/detail").param("memoryId", memory.toString()).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mediaCount").value(0));
        upload(memory).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEDIA_004"));
        verify(uploader, times(1)).upload(any(byte[].class), anyMap());
    }

    @Test void providerFailureKeepsPhotoAndRequestAndManualRetryCompletes() throws Exception {
        UUID memory = create(), photo = uploadedPhoto(memory);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("fixture failure"));
        erase(photo).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("MEDIA_002"));
        assertThat(count("media", memory)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT completed_at IS NULL AND attempts=1 FROM media_deletions WHERE media_id=?", Boolean.class, photo)).isTrue();
        storageDeletesSuccessfully();
        erase(photo).andExpect(status().isNoContent());
        assertThat(count("media", memory)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_deletions WHERE media_id=?", Long.class, photo)).isEqualTo(1);
    }

    @Test void cloudSuccessFollowedByDatabaseFailureRetainsTheReceiptForSafeRetry() throws Exception {
        UUID memory = create(), photo = uploadedPhoto(memory);
        storageDeletesSuccessfully();
        jdbc.execute("CREATE FUNCTION media_delete_fixture_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
                + "IF OLD.id='" + photo + "'::uuid THEN RAISE EXCEPTION 'fixture deletion failure'; END IF; RETURN OLD; END; $$");
        jdbc.execute("CREATE TRIGGER media_delete_fixture BEFORE DELETE ON media FOR EACH ROW EXECUTE FUNCTION media_delete_fixture_failure()");
        try {
            erase(photo).andExpect(status().isInternalServerError());
            assertThat(count("media", memory)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT completed_at IS NULL FROM media_deletions WHERE media_id=?", Boolean.class, photo)).isTrue();
        } finally {
            jdbc.execute("DROP TRIGGER media_delete_fixture ON media");
            jdbc.execute("DROP FUNCTION media_delete_fixture_failure()");
        }
        doReturn(Map.of("result", "not found")).when(uploader).destroy(anyString(), anyMap());
        erase(photo).andExpect(status().isNoContent());
        assertThat(count("media", memory)).isZero();
    }

    @Test void unknownProviderResponseFailsAndNotFoundOnRetryIsAccepted() throws Exception {
        UUID memory = create(), photo = uploadedPhoto(memory);
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result", "unexpected"));
        erase(photo).andExpect(status().isServiceUnavailable());
        assertThat(count("media", memory)).isEqualTo(1);
        doReturn(Map.of("result", "not found")).when(uploader).destroy(anyString(), anyMap());
        erase(photo).andExpect(status().isNoContent());
        assertThat(count("media", memory)).isZero();
    }

    @Test void persistedPendingDeletionIsRecoveredByCleanup() throws Exception {
        UUID memory = create(), photo = uploadedPhoto(memory);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("fixture failure"));
        erase(photo).andExpect(status().isServiceUnavailable());
        jdbc.update("UPDATE media_deletions SET next_attempt_at=now()-interval '1 second' WHERE media_id=?", photo);
        storageDeletesSuccessfully();
        deletion.retryPending();
        assertThat(count("media", memory)).isZero();
        assertThat(jdbc.queryForObject("SELECT completed_at IS NOT NULL FROM media_deletions WHERE media_id=?", Boolean.class, photo)).isTrue();
    }

    @Test void deletionRequiresAuthenticationAndOwnershipBeforeCloud() throws Exception {
        UUID memory = create(), photo = uploadedPhoto(memory);
        jdbc.update("UPDATE memories SET user_id=? WHERE id=?", otherOwner(), memory);
        erase(photo).andExpect(status().isNotFound());
        erase(UUID.randomUUID()).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/media").param("id", photo.toString())).andExpect(status().isUnauthorized());
        verify(uploader, never()).destroy(anyString(), anyMap());
        assertThat(count("media", memory)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_deletions WHERE media_id=?", Long.class, photo)).isZero();
    }

    @Test void deletingOnlyPhotoArchivesEmptyMemoryAndPreservesNoteMemories() throws Exception {
        var body = request(); body.put("note", "");
        UUID memory = UUID.fromString(data(create(body).andExpect(status().isCreated())).path("id").asText());
        UUID photo = uploadedPhoto(memory);
        storageDeletesSuccessfully();
        erase(photo).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM memories WHERE id=?", Boolean.class, memory)).isTrue();
        erase(photo).andExpect(status().isNoContent());
    }

    @Test void deletingMemoryOrTripEventuallyCleansCloudAssets() throws Exception {
        UUID memory = create(); uploadedPhoto(memory);
        mvc.perform(delete("/api/v1/journal/memories").param("id", memory.toString()).header("Authorization", bearer()))
                .andExpect(status().isNoContent());
        storageDeletesSuccessfully();
        deletion.retryPending();
        assertThat(count("media", memory)).isZero();
        UUID second = create(); uploadedPhoto(second);
        mvc.perform(delete("/api/v1/trips/" + trip).param("version", "0").header("Authorization", bearer()))
                .andExpect(status().isNoContent());
        deletion.retryPending();
        assertThat(count("media", second)).isZero();
    }

    @Test void oldCloudinaryPhotoWithoutIdentityIsRetainedRatherThanGuessing() throws Exception {
        UUID memory = create(), photo = uploadedPhoto(memory);
        jdbc.update("UPDATE media SET public_id=NULL WHERE id=?", photo);
        erase(photo).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEDIA_003"));
        assertThat(count("media", memory)).isEqualTo(1);
        verify(uploader, never()).destroy(anyString(), anyMap());
    }

    @Test void concurrentDeletesConvergeOnOneReceipt() throws Exception {
        UUID memory = create(), photo = uploadedPhoto(memory);
        var barrier = new CyclicBarrier(2);
        when(uploader.destroy(anyString(), anyMap())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            barrier.await(10, TimeUnit.SECONDS);
            return Map.of("result", "ok");
        });
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> erase(photo).andExpect(status().isNoContent()));
            var second = pool.submit(() -> erase(photo).andExpect(status().isNoContent()));
            first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
        }
        assertThat(count("media", memory)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_deletions WHERE media_id=?", Long.class, photo)).isEqualTo(1);
    }

    @Test void uploadFinishingAfterMemoryDeletionIsCleanedWithoutAttachingAnAsset() throws Exception {
        UUID memory = create();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        when(uploader.upload(any(byte[].class), anyMap())).thenAnswer(invocation -> {
            entered.countDown(); assertThat(release.await(20, TimeUnit.SECONDS)).isTrue();
            Map<?, ?> parameters = invocation.getArgument(1);
            return Map.of("secure_url", "https://res.cloudinary.com/fixture/image/upload/photo.jpg",
                    "public_id", parameters.get("folder") + "/" + parameters.get("public_id"),
                    "bytes", image.length, "width", 8, "height", 6);
        });
        storageDeletesSuccessfully();
        try (var pool = Executors.newSingleThreadExecutor()) {
            var pending = pool.submit(() -> upload(memory));
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
                mvc.perform(delete("/api/v1/journal/memories").param("id", memory.toString()).header("Authorization", bearer()))
                        .andExpect(status().isNoContent());
            } finally { release.countDown(); }
            pending.get(20, TimeUnit.SECONDS).andExpect(status().isNotFound());
        }
        assertThat(count("media", memory)).isZero();
        verify(uploader, times(1)).destroy(anyString(), anyMap());
        assertThat(jdbc.queryForObject("SELECT completed_at IS NOT NULL FROM media_deletions WHERE memory_id=?", Boolean.class, memory)).isTrue();
    }

    @Test void uploadInFlightCannotResurrectDeletedPhoto() throws Exception {
        UUID memory = create();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        when(uploader.upload(any(byte[].class), anyMap())).thenAnswer(invocation -> {
            if (calls.getAndIncrement() == 0) {
                entered.countDown(); assertThat(release.await(20, TimeUnit.SECONDS)).isTrue();
            }
            Map<?, ?> parameters = invocation.getArgument(1);
            return Map.of("secure_url", "https://res.cloudinary.com/fixture/image/upload/photo.jpg",
                    "public_id", parameters.get("folder") + "/" + parameters.get("public_id"),
                    "bytes", image.length, "width", 8, "height", 6);
        });
        storageDeletesSuccessfully();
        try (var pool = Executors.newSingleThreadExecutor()) {
            var first = pool.submit(() -> upload(memory));
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
                UUID photo = uploadedPhoto(memory);
                erase(photo).andExpect(status().isNoContent());
            } finally { release.countDown(); }
            first.get(20, TimeUnit.SECONDS).andExpect(status().isConflict());
        }
        assertThat(count("media", memory)).isZero();
        verify(uploader, times(2)).destroy(anyString(), anyMap());
        assertThat(jdbc.queryForObject("SELECT completed_at IS NOT NULL FROM media_deletions WHERE memory_id=?", Boolean.class, memory)).isTrue();
    }
}
