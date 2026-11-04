package com.travel.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
import com.travel.memory.entity.Memory;
import com.travel.memory.repository.MemoryRepository;
import com.travel.place.entity.Place;
import com.travel.place.entity.Province;
import com.travel.place.repository.PlaceRepository;
import com.travel.place.repository.ProvinceRepository;
import com.travel.trip.repository.TripPlaceRepository;
import com.travel.trip.repository.TripRepository;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TripIntegrationTest {
    private static final String TRIPS = "/api/v1/trips";

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired TripRepository trips;
    @Autowired PlaceRepository places;
    @Autowired ProvinceRepository provinces;
    @Autowired TripPlaceRepository tripPlaces;
    @Autowired MemoryRepository memories;
    @MockitoBean AuthEmailSender emailSender;

    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private User owner;
    private String token;

    @BeforeEach
    void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String password = UUID.randomUUID().toString();
        owner = users.saveAndFlush(User.builder().fullName("Trip Owner")
                .email(UUID.randomUUID() + "@example.com").passwordHash(passwords.encode(password))
                .role(Role.USER).emailVerifiedAt(Instant.now()).build());
        token = login(owner, password);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void foreignAndMissingTripsArePrivateAcrossAllOperations() throws Exception {
        String otherPassword = UUID.randomUUID().toString();
        User other = users.saveAndFlush(User.builder().fullName("Other Owner")
                .email(UUID.randomUUID() + "@example.com").passwordHash(passwords.encode(otherPassword))
                .role(Role.USER).emailVerifiedAt(Instant.now()).build());
        String otherToken = login(other, otherPassword);
        String tripId = createTrip();
        Place place = place();
        send(post(TRIPS + "/" + tripId + "/places"), Map.of("placeId", place.getId()));
        for (String id : List.of(tripId, UUID.randomUUID().toString())) {
            String path = TRIPS + "/" + id;
            List<MockHttpServletRequestBuilder> requests = List.of(
                    get(path), patch(path).content(json.writeValueAsString(Map.of("version", 0, "title", "Stolen"))), delete(path).param("version", "0"),
                    get(path + "/places"), post(path + "/places").content(json.writeValueAsString(Map.of("placeId", place.getId()))),
                    put(path + "/places/" + place.getId()).content(json.writeValueAsString(Map.of("visited", true))),
                    delete(path + "/places/" + place.getId()));
            for (var request : requests) {
                send(request, otherToken).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TRIP_001"));
            }
        }
        assertThat(data(send(get(TRIPS), otherToken).andExpect(status().isOk())).path("content")).isEmpty();
        send(get(TRIPS + "/" + tripId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trip.title").value("Weekend trip"));
    }

    @Test
    void unauthenticatedRequestsCannotReadOrMutateTrips() throws Exception {
        String path = TRIPS + "/" + createTrip();
        for (var request : List.of(get(TRIPS), get(path), post(TRIPS).content(json.writeValueAsString(tripBody())),
                patch(path).content("{}"), delete(path), get(path + "/places"),
                post(path + "/places").content("{}"), put(path + "/places/" + UUID.randomUUID()).content("{}"),
                delete(path + "/places/" + UUID.randomUUID()))) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void addExistingAndNewPlacesAndUpdateOnlyTripMetadata() throws Exception {
        String tripId = createTrip();
        String path = TRIPS + "/" + tripId + "/places";
        Place existing = place();
        send(post(path), Map.of("placeId", existing.getId(), "priority", 2, "estimatedDuration", 45,
                        "notes", "Visit before lunch", "plannedOrder", 1, "tripId", UUID.randomUUID(), "userId", UUID.randomUUID()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.placeId").value(existing.getId().toString()))
                .andExpect(jsonPath("$.data.tripId").value(tripId))
                .andExpect(jsonPath("$.data.place.name").value(existing.getName()))
                .andExpect(jsonPath("$.data.visited").value(false));
        JsonNode created = data(send(post(path), Map.of("name", "New viewpoint", "latitude", 16.05,
                        "longitude", 108.2, "providerPlaceId", "provider-" + UUID.randomUUID(), "address", "Hill road"))
                .andExpect(status().isCreated()));
        assertThat(places.findById(UUID.fromString(created.path("placeId").asText()))).isPresent();
        send(put(path + "/" + existing.getId()), Map.of("visited", true, "notes", "Visited", "plannedOrder", 0))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visited").value(true))
                .andExpect(jsonPath("$.data.priority").value(2))
                .andExpect(jsonPath("$.data.estimatedDuration").value(45));
        JsonNode list = data(send(get(path)).andExpect(status().isOk()));
        assertThat(list).hasSize(2);
        assertThat(list.get(0).path("placeId").asText()).isEqualTo(existing.getId().toString());
        send(get(TRIPS + "/" + tripId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trip.stopCount").value(2));
        assertThat(places.findById(existing.getId()).orElseThrow().getName()).isEqualTo(existing.getName());
    }

    @Test
    void duplicateLinkReturnsConflictAndDoesNotDuplicateData() throws Exception {
        String tripId = createTrip();
        Place place = place();
        String path = TRIPS + "/" + tripId + "/places";
        send(post(path), Map.of("placeId", place.getId())).andExpect(status().isCreated());
        send(post(path), Map.of("placeId", place.getId())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_003"));
        assertThat(tripPlaces.findByTripId(UUID.fromString(tripId))).hasSize(1);
    }

    @Test
    void newPlacesWithSameProviderIdRemainSeparateRecords() throws Exception {
        String path = TRIPS + "/" + createTrip() + "/places";
        var body = Map.of("name", "Provider location", "latitude", 16.06, "longitude", 108.24,
                "providerPlaceId", "provider-" + UUID.randomUUID());
        JsonNode first = data(send(post(path), body).andExpect(status().isCreated()));
        JsonNode second = data(send(post(path), body).andExpect(status().isCreated()));
        assertThat(first.path("placeId").asText()).isNotEqualTo(second.path("placeId").asText());
        assertThat(data(send(get(path)).andExpect(status().isOk()))).hasSize(2);
    }

    @Test
    void concurrentDuplicateAddsHaveOneWinner() throws Exception {
        String tripId = createTrip();
        Place place = place();
        String path = TRIPS + "/" + tripId + "/places";
        assertThat(race(() -> send(post(path), Map.of("placeId", place.getId())).andReturn().getResponse().getStatus()))
                .containsExactlyInAnyOrder(201, 409);
        assertThat(tripPlaces.findByTripId(UUID.fromString(tripId))).hasSize(1);
    }

    @Test
    void reorderingAndRemovingPlacesKeepContiguousRanks() throws Exception {
        String path = TRIPS + "/" + createTrip() + "/places";
        List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ids.add(data(send(post(path), Map.of("name", "Stop " + i, "latitude", 16.0, "longitude", 108.0))
                    .andExpect(status().isCreated())).path("placeId").asText());
        }
        send(put(path + "/" + ids.get(2)), Map.of("plannedOrder", 0)).andExpect(status().isOk());
        JsonNode reordered = data(send(get(path)).andExpect(status().isOk()));
        assertThat(reordered).extracting(node -> node.path("placeId").asText())
                .containsExactly(ids.get(2), ids.get(0), ids.get(1));
        send(put(path + "/" + ids.get(0)), Map.of("plannedOrder", 100)).andExpect(status().isOk());
        JsonNode clamped = data(send(get(path)).andExpect(status().isOk()));
        assertThat(clamped).extracting(node -> node.path("placeId").asText())
                .containsExactly(ids.get(2), ids.get(1), ids.get(0));
        send(delete(path + "/" + ids.get(2))).andExpect(status().isNoContent());
        JsonNode remaining = data(send(get(path)).andExpect(status().isOk()));
        assertThat(remaining).extracting(node -> node.path("plannedOrder").asInt()).containsExactly(0, 1);
    }

    @Test
    void updatingOneTripsMetadataDoesNotChangeAnotherTripsSharedPlace() throws Exception {
        Place shared = place();
        String first = TRIPS + "/" + createTrip() + "/places";
        String second = TRIPS + "/" + createTrip() + "/places";
        send(post(first), Map.of("placeId", shared.getId(), "notes", "First trip")).andExpect(status().isCreated());
        send(post(second), Map.of("placeId", shared.getId(), "notes", "Second trip")).andExpect(status().isCreated());
        send(put(first + "/" + shared.getId()), Map.of("notes", "Changed", "visited", true)).andExpect(status().isOk());
        send(get(second)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].notes").value("Second trip"))
                .andExpect(jsonPath("$.data[0].visited").value(false));
        send(delete(first + "/" + shared.getId())).andExpect(status().isNoContent());
        send(get(second)).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].place.name").value(shared.getName()));
    }

    @Test
    void invalidCoordinatesAndMetadataAreRejectedBeforePersistence() throws Exception {
        String path = TRIPS + "/" + createTrip() + "/places";
        for (var invalid : List.of(Map.of("name", "Invalid", "latitude", 91, "longitude", 108),
                Map.of("name", "Invalid", "latitude", 16, "longitude", -181),
                Map.of("name", "Invalid", "latitude", 16),
                Map.of("name", " ", "latitude", 16, "longitude", 108),
                Map.of("name", "Invalid", "latitude", 16, "longitude", 108, "estimatedDuration", -1))) {
            send(post(path), invalid).andExpect(status().isBadRequest());
        }
        assertThat(data(send(get(path)).andExpect(status().isOk()))).isEmpty();
    }

    @Test
    void missingPlaceAndMissingLinkHaveDistinctNotFoundErrors() throws Exception {
        String path = TRIPS + "/" + createTrip() + "/places";
        send(post(path), Map.of("placeId", UUID.randomUUID())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_001"));
        Place notLinked = place();
        send(put(path + "/" + notLinked.getId()), Map.of("visited", true)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_002"));
        send(delete(path + "/" + notLinked.getId())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_002"));
    }

    @Test
    void missingProvinceRollsBackPlaceAndValidProvinceIsReturned() throws Exception {
        String path = TRIPS + "/" + createTrip() + "/places";
        long originalPlaceCount = places.count();
        send(post(path), Map.of("name", "Province location", "latitude", 16.0, "longitude", 108.0,
                        "provinceId", UUID.randomUUID(), "category", "nature"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PLACE_002"));
        assertThat(places.count()).isEqualTo(originalPlaceCount);
        assertThat(data(send(get(path)).andExpect(status().isOk()))).isEmpty();

        Province province = provinces.saveAndFlush(Province.builder()
                .code(UUID.randomUUID().toString().substring(0, 8)).name("Test Province").build());
        send(post(path), Map.of("name", "Province location", "latitude", 16.0, "longitude", 108.0,
                        "provinceId", province.getId(), "category", "nature"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.place.provinceId").value(province.getId().toString()))
                .andExpect(jsonPath("$.data.place.provinceName").value("Test Province"))
                .andExpect(jsonPath("$.data.place.category").value("nature"));
        assertThat(places.count()).isEqualTo(originalPlaceCount + 1);
    }

    @Test
    void tripCountsOnlyIncludeOwnersActiveMemories() throws Exception {
        String tripId = createTrip();
        UUID id = UUID.fromString(tripId);
        Place place = place();
        memories.saveAndFlush(Memory.create(owner.getId(), id, place.getId(), "Active memory", Instant.now()));
        Memory deleted = Memory.create(owner.getId(), id, place.getId(), "Deleted memory", Instant.now());
        deleted.softDelete();
        memories.saveAndFlush(deleted);
        User other = users.saveAndFlush(User.builder().fullName("Other memory owner")
                .email(UUID.randomUUID() + "@example.com").role(Role.USER).emailVerifiedAt(Instant.now()).build());
        memories.saveAndFlush(Memory.create(other.getId(), id, place.getId(), "Not the trip owner's memory", Instant.now()));

        send(get(TRIPS + "/" + tripId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.trip.memoryCount").value(1));
        send(get(TRIPS)).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].memoryCount").value(1));
    }

    @Test
    void removingTripPlacePreservesSharedPlaceAndMemories() throws Exception {
        String tripId = createTrip();
        Place place = place();
        String path = TRIPS + "/" + tripId + "/places";
        send(post(path), Map.of("placeId", place.getId())).andExpect(status().isCreated());
        Memory memory = memories.saveAndFlush(Memory.create(owner.getId(), UUID.fromString(tripId),
                place.getId(), "Keep this memory", Instant.now()));
        send(delete(path + "/" + place.getId())).andExpect(status().isNoContent());
        assertThat(places.findById(place.getId())).isPresent();
        assertThat(memories.findByIdAndDeletedAtIsNull(memory.getId())).isPresent();
        send(get(TRIPS + "/" + tripId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trip.stopCount").value(0)).andExpect(jsonPath("$.data.trip.memoryCount").value(1));
    }

    private Map<String, Object> tripBody() {
        return new LinkedHashMap<>(Map.of("clientRequestId", UUID.randomUUID(), "title", "Weekend trip", "destination", Map.of("name", "Da Nang"),
                "startDate", "2027-01-10", "endDate", "2027-01-12"));
    }

    private String createTrip() throws Exception {
        return data(send(post(TRIPS), tripBody()).andExpect(status().isCreated())).path("trip").path("id").asText();
    }

    private Place place() {
        return places.saveAndFlush(Place.builder().name("Shared beach").latitude(16.06).longitude(108.24).build());
    }

    private String login(User user, String password) throws Exception {
        return data(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", password))))
                .andExpect(status().isOk())).path("accessToken").asText();
    }

    private ResultActions send(MockHttpServletRequestBuilder request) throws Exception {
        return send(request, token);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return send(request.content(json.writeValueAsString(body)), token);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String accessToken) throws Exception {
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + accessToken));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }

    private List<Integer> race(Callable<Integer> action) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> runner = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new TimeoutException("Race did not start");
                try { return action.call(); } finally { SecurityContextHolder.clearContext(); }
            };
            var first = executor.submit(runner);
            var second = executor.submit(runner);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        }
    }
}
