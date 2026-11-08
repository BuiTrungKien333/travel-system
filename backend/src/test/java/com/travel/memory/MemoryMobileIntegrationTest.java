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
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MemoryMobileIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean AuthEmailSender mail;
    @MockitoBean Cloudinary cloudinary;
    MockMvc mvc;
    String token;
    UUID owner;

    @BeforeEach void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String password = UUID.randomUUID() + ".Ab1";
        User user = users.save(User.builder().fullName("Memory owner").email(UUID.randomUUID() + "@example.com")
                .passwordHash(encoder.encode(password)).emailVerifiedAt(Instant.parse("2026-01-01T00:00:00Z")).build());
        owner = user.getId();
        token = data(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", password)))).andExpect(status().isOk()))
                .path("accessToken").asText();
    }
    Map<String, Object> memoryRequest() {
        return Map.of("clientRequestId", UUID.randomUUID(), "note", "Original note", "experiencedAt", "2026-01-01T00:00:00Z");
    }
    ResultActions create(Map<String, Object> request) throws Exception {
        return mvc.perform(post("/api/v1/journal/memories").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)));
    }
    JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }

    @Test void createReplayDetailAndVersionedNoteUpdateUseTheMobileContract() throws Exception {
        var body = memoryRequest();
        JsonNode created = data(create(body).andExpect(status().isCreated()).andExpect(jsonPath("$.data.version").value(0)));
        String id = created.path("id").asText();
        create(body).andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(id));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM memories WHERE user_id=?", Long.class, owner)).isEqualTo(1);
        mvc.perform(get("/api/v1/journal/memories/detail").param("memoryId", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.note").value("Original note"));
        mvc.perform(patch("/api/v1/journal/memories/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"note\":\"Updated\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1)).andExpect(jsonPath("$.data.note").value("Updated"));
        mvc.perform(patch("/api/v1/journal/memories/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"note\":\"Stale\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMORY_003"));
        var changed = new java.util.HashMap<>(body); changed.put("note", "Conflicting create");
        create(changed).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMORY_004"));
        mvc.perform(delete("/api/v1/journal/memories").param("id", id).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        create(body).andExpect(status().isNotFound());
    }

    @Test void concurrentCreateReplaysPersistOnlyOneMemory() throws Exception {
        var body = memoryRequest();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<String> action = () -> { start.await(); return data(create(body).andExpect(status().isCreated())).path("id").asText(); };
            Future<String> first = pool.submit(action), second = pool.submit(action); start.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(second.get(20, TimeUnit.SECONDS));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM memories WHERE user_id=?", Long.class, owner)).isEqualTo(1);
    }

    @Test void uploadWithBothTripAndMemoryIdsIsAttachedAndVisibleOnReload() throws Exception {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        String tripId = data(mvc.perform(post("/api/v1/trips").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("clientRequestId", UUID.randomUUID(),
                        "destination", Map.of("name", "Đà Lạt"), "startDate", date.toString(), "endDate", date.toString()))))
                .andExpect(status().isCreated())).path("trip").path("id").asText();
        var request = new java.util.HashMap<>(memoryRequest()); request.put("tripId", tripId);
        String memoryId = data(create(request).andExpect(status().isCreated())).path("id").asText();
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://example.com/photo.png",
                "public_id", "test/photo", "bytes", 8, "width", 1, "height", 1));
        mvc.perform(get("/api/v1/media/limits").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.maxFileBytes").value(10485760));
        mvc.perform(multipart("/api/v1/media/upload").file(new MockMultipartFile("file", "photo.png", "image/png", png(1)))
                .param("tripId", tripId).param("memoryId", memoryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.mediaId").isNotEmpty());
        mvc.perform(get("/api/v1/journal/memories/detail").param("memoryId", memoryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mediaCount").value(1))
                .andExpect(jsonPath("$.data.media[0].url").value("https://example.com/photo.png"));
        verify(uploader, times(1)).upload(any(), anyMap());
    }

    @Test void concurrentCaptureUploadReplaysOneMediaAndRejectsChangedContent() throws Exception {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        String tripId = data(mvc.perform(post("/api/v1/trips").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("clientRequestId", UUID.randomUUID(),
                        "destination", Map.of("name", "Da Lat"), "startDate", date.toString(), "endDate", date.toString()))))
                .andExpect(status().isCreated())).path("trip").path("id").asText();
        var request = new java.util.HashMap<>(memoryRequest()); request.put("tripId", tripId);
        String memoryId = data(create(request).andExpect(status().isCreated())).path("id").asText();
        String key = UUID.randomUUID().toString();
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenAnswer(invocation -> {
            Thread.sleep(100);
            return Map.of("secure_url", "https://example.com/replay.png", "public_id", "test/replay", "bytes", 2, "width", 1, "height", 1);
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<String> upload = () -> data(mvc.perform(multipart("/api/v1/media/upload")
                    .file(new MockMultipartFile("file", "photo.png", "image/png", png(1)))
                    .param("tripId", tripId).param("memoryId", memoryId).param("clientRequestId", key)
                    .header("Authorization", "Bearer " + token)).andExpect(status().isCreated())).path("mediaId").asText();
            var futures = executor.invokeAll(List.of(upload, upload));
            assertThat(futures.get(0).get()).isEqualTo(futures.get(1).get());
        } finally { executor.shutdownNow(); }
        mvc.perform(multipart("/api/v1/media/upload")
                .file(new MockMultipartFile("file", "photo.png", "image/png", png(2)))
                .param("tripId", tripId).param("memoryId", memoryId).param("clientRequestId", key)
                .header("Authorization", "Bearer " + token)).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/journal/memories/detail").param("memoryId", memoryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.mediaCount").value(1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media WHERE memory_id=?", Long.class, UUID.fromString(memoryId))).isEqualTo(1);
        verify(uploader, times(1)).upload(any(), anyMap());
        UUID other = users.save(User.builder().fullName("Other").email(UUID.randomUUID() + "@example.com").build()).getId();
        jdbc.update("UPDATE memories SET user_id=? WHERE id=?", other, UUID.fromString(memoryId));
        mvc.perform(multipart("/api/v1/media/upload")
                .file(new MockMultipartFile("file", "photo.png", "image/png", png(1)))
                .param("tripId", tripId).param("memoryId", memoryId).param("clientRequestId", key)
                .header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test void updatesCannotModifyOtherUsersMemoriesAndValidateInputs() throws Exception {
        String id = data(create(memoryRequest()).andExpect(status().isCreated())).path("id").asText();
        UUID other = users.save(User.builder().fullName("Other").email(UUID.randomUUID() + "@example.com").build()).getId();
        jdbc.update("UPDATE memories SET user_id=? WHERE id=?", other, UUID.fromString(id));
        mvc.perform(patch("/api/v1/journal/memories/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"note\":\"Other\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/journal/memories/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Missing version\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/journal/memories/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"note\":\"Anonymous\"}")).andExpect(status().isUnauthorized());
    }

    @Test void exportActualResponsesForTheMobileContractCheck() throws Exception {
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        ResultActions created = mvc.perform(post("/api/v1/trips").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("clientRequestId", UUID.randomUUID(),
                        "title", "Mobile contract", "destination", Map.of("name", "Đà Lạt"), "startDate", date.toString(), "endDate", date.toString()))))
                .andExpect(status().isCreated());
        String tripId = data(created).path("trip").path("id").asText();
        ResultActions dayUpdated = mvc.perform(put("/api/v1/trips/" + tripId + "/days/" + date)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("version", 0, "startLocation", "Mobile hotel", "startTime", "07:45",
                        "returnToStart", true, "transport", "MOTORCYCLE", "startCoordinate", Map.of("latitude",11.94,"longitude",108.44), "durations", List.of()))))
                .andExpect(status().isOk());
        ResultActions addedPlace = mvc.perform(post("/api/v1/trips/" + tripId + "/places")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", "Pinned fixture", "latitude", 11.94, "longitude", 108.44))))
                .andExpect(status().isCreated());
        String placeId = data(addedPlace).path("placeId").asText();
        mvc.perform(put("/api/v1/trips/" + tripId + "/itinerary").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("days", List.of(Map.of("date", date.toString(),
                        "stops", List.of(Map.of("placeId", placeId, "sequence", 0, "arrivalTime", date.atTime(10,0).toString(),
                                "durationMinutes", 45, "pinned", true))))))))
                .andExpect(status().isOk());
        var memoryBody = new java.util.HashMap<>(memoryRequest()); memoryBody.put("tripId", tripId);
        ResultActions memory = create(memoryBody).andExpect(status().isCreated());
        String memoryId = data(memory).path("id").asText();
        Map<String, JsonNode> responses = new java.util.LinkedHashMap<>();
        responses.put("tripCreate", json.readTree(created.andReturn().getResponse().getContentAsString()));
        responses.put("tripDayUpdate", json.readTree(dayUpdated.andReturn().getResponse().getContentAsString()));
        for (var entry : Map.of("tripDetail", "/api/v1/trips/" + tripId, "tripList", "/api/v1/trips?size=100",
                "destinations", "/api/v1/destinations?size=100", "limits", "/api/v1/media/limits",
                "memoryDetail", "/api/v1/journal/memories/detail?memoryId=" + memoryId, "memoryList", "/api/v1/journal/memories",
                "tripPlaces", "/api/v1/trips/" + tripId + "/places", "itinerary", "/api/v1/trips/" + tripId + "/itinerary").entrySet()) {
            responses.put(entry.getKey(), json.readTree(mvc.perform(get(entry.getValue()).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));
        }
        responses.put("memoryCreate", json.readTree(memory.andReturn().getResponse().getContentAsString()));
        responses.put("profileUpdate", json.readTree(mvc.perform(put("/api/v1/users/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Updated mobile user\",\"avatarUrl\":\"\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target", "mobile-api-contract.json"), json.writeValueAsString(responses));
    }
    private byte[] png(int color) throws Exception {
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, color);
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }
}
