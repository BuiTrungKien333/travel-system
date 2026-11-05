package com.travel.trip;

import com.fasterxml.jackson.databind.*;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TripCrudIntegrationTest.FixedTime.class})
class TripMutationIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean AuthEmailSender sender;
    MockMvc mvc;
    UUID owner;
    String token;
    @BeforeEach void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var user = users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.com").fullName("Fixture")
                .passwordHash(encoder.encode("fixture-password")).emailVerifiedAt(Instant.now()).build());
        owner = user.getId();
        token = read(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",user.getEmail(),"password","fixture-password"))))
                .andExpect(status().isOk())).path("accessToken").asText();
    }
    JsonNode read(ResultActions result) throws Exception { return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data"); }
    JsonNode create() throws Exception {
        return read(mvc.perform(post("/api/v1/trips").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("clientRequestId",UUID.randomUUID(),"destination",Map.of("name","Đà Lạt"),
                        "startDate","2026-11-20","endDate","2026-11-22","notes","Original","budget",Map.of("amount",1000,"currency","VND")))))
                .andExpect(status().isCreated())).path("trip");
    }
    ResultActions patchTrip(String id, Map<String,?> body) throws Exception {
        return mvc.perform(patch("/api/v1/trips/"+id).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    ResultActions preview(String id, long version, String start, String end) throws Exception {
        return mvc.perform(post("/api/v1/trips/"+id+"/date-change-preview").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("version",version,"startDate",start,"endDate",end))));
    }
    ResultActions deleteTrip(String id, long version) throws Exception {
        return mvc.perform(delete("/api/v1/trips/"+id).param("version",Long.toString(version)).header("Authorization","Bearer "+token));
    }
    UUID place() {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO places(id,name,latitude,longitude) VALUES (?,'Fixture',11.94,108.44)",id);
        return id;
    }
    UUID schedule(String trip, UUID place, String date, boolean membership) {
        UUID tripId=UUID.fromString(trip), item=UUID.randomUUID();
        if (membership) jdbc.update("INSERT INTO trip_places(trip_id,place_id,notes,estimated_duration,visited) VALUES (?,?,'Place note',75,true) ON CONFLICT DO NOTHING",tripId,place);
        UUID itinerary=jdbc.queryForObject("SELECT id FROM itineraries WHERE trip_id=?",UUID.class,tripId);
        jdbc.update("INSERT INTO itinerary_items(id,itinerary_id,place_id,date,start_time,end_time,sequence,note) VALUES (?,?,?,?::date,'09:00','10:30',0,'Appointment note')",item,itinerary,place,date);
        return item;
    }
    Map<String,Object> dayBody(long version) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("version", version); body.put("startLocation", "  Hotel  "); body.put("startTime", "07:45");
        body.put("returnToStart", true); body.put("transport", "MOTORCYCLE");
        return body;
    }
    ResultActions putDay(String id, String date, Map<String,?> body) throws Exception {
        return mvc.perform(put("/api/v1/trips/"+id+"/days/"+date).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }
    java.time.Instant scheduleUpdatedAt(String id) {
        return jdbc.queryForObject("SELECT updated_at FROM itineraries WHERE trip_id=?", java.sql.Timestamp.class, UUID.fromString(id)).toInstant();
    }
    @Test void emptyDaySettingsRoundTripWithoutLosingOtherDaysAndNoOpPreservesVersion() throws Exception {
        String id = create().path("id").asText();
        var before = scheduleUpdatedAt(id);
        var changed = read(putDay(id, "2026-11-20", dayBody(0)).andExpect(status().isOk())).path("trip");
        assertThat(changed.path("version").asLong()).isEqualTo(1);
        assertThat(changed.path("days").get(0).path("startLocation").asText()).isEqualTo("Hotel");
        assertThat(changed.path("days").get(0).path("startTime").asText()).isEqualTo("07:45");
        assertThat(changed.path("days").get(0).path("returnToStart").asBoolean()).isTrue();
        assertThat(changed.path("days").get(0).path("transport").asText()).isEqualTo("MOTORCYCLE");
        assertThat(changed.path("days").get(1).path("startLocation").isNull()).isTrue();
        assertThat(changed.path("days").get(1).path("transport").asText()).isEqualTo("CAR");
        assertThat(scheduleUpdatedAt(id)).isAfter(before);
        mvc.perform(get("/api/v1/trips/"+id).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.trip.days[0].startLocation").value("Hotel"));
        putDay(id, "2026-11-20", dayBody(1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.trip.version").value(1));
        var clear = dayBody(1); clear.put("startLocation", " "); clear.put("startTime", null);
        clear.put("returnToStart", false); clear.put("transport", "WALK");
        putDay(id, "2026-11-20", clear).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trip.days[0].startLocation").isEmpty())
                .andExpect(jsonPath("$.data.trip.days[0].startTime").isEmpty())
                .andExpect(jsonPath("$.data.trip.days[0].transport").value("WALK"));
    }
    @Test void dayAndDurationsSaveTogetherWithoutReplacingScheduleOrPlaceMetadata() throws Exception {
        String id = create().path("id").asText(); UUID place = place(); UUID item = schedule(id, place, "2026-11-20", true);
        var body = dayBody(0);
        body.put("expectedItineraryUpdatedAt", scheduleUpdatedAt(id).toString());
        body.put("durations", List.of(Map.of("stopId", place, "durationMinutes", 95)));
        putDay(id, "2026-11-20", body).andExpect(status().isOk()).andExpect(jsonPath("$.data.trip.version").value(1));
        assertThat(jdbc.queryForObject("SELECT duration_minutes FROM itinerary_items WHERE id=?", Integer.class, item)).isEqualTo(95);
        assertThat(jdbc.queryForObject("SELECT end_time FROM itinerary_items WHERE id=?", java.sql.Time.class, item).toString()).isEqualTo("10:35:00");
        assertThat(jdbc.queryForObject("SELECT note FROM itinerary_items WHERE id=?", String.class, item)).isEqualTo("Appointment note");
        assertThat(jdbc.queryForObject("SELECT notes FROM trip_places WHERE trip_id=? AND place_id=?", String.class, UUID.fromString(id), place)).isEqualTo("Place note");
        assertThat(jdbc.queryForObject("SELECT visited FROM trip_places WHERE trip_id=? AND place_id=?", Boolean.class, UUID.fromString(id), place)).isTrue();
        mvc.perform(get("/api/v1/trips/"+id+"/itinerary").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.days[0].stops[0].durationMinutes").value(95));
        var extended = read(patchTrip(id, Map.of("version", 1, "startDate", "2026-11-20", "endDate", "2026-11-23"))
                .andExpect(status().isOk())).path("trip");
        assertThat(extended.path("days").get(0).path("transport").asText()).isEqualTo("MOTORCYCLE");
        assertThat(extended.path("days").get(3).path("transport").asText()).isEqualTo("CAR");
    }
    @Test void dayRejectsInvalidInputOrDatesWithoutChangingVersion() throws Exception {
        String id = create().path("id").asText();
        for (var invalid : List.of(Map.of("startTime", "24:00"), Map.of("startTime", "8:00"), Map.of("transport", "FLY"),
                Map.of("version", -1), Map.of("version", 0.5), Map.of("version", "0"), Map.of("startLocation", ""),
                Map.of("startLocation", "X".repeat(201)))) {
            var body = dayBody(0); body.putAll(invalid);
            putDay(id, "2026-11-20", body).andExpect(status().isBadRequest());
        }
        var missing = dayBody(0); missing.remove("version");
        putDay(id, "2026-11-20", missing).andExpect(status().isBadRequest());
        putDay(id, "2026-11-19", dayBody(0)).andExpect(status().isBadRequest());
        putDay(id, "2026-11-23", dayBody(0)).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT version FROM trips WHERE id=?", Long.class, UUID.fromString(id))).isZero();
    }
    @Test void staleScheduleOrMissingStopDoesNotPartiallySaveDaySettings() throws Exception {
        String id = create().path("id").asText(); UUID place = place(); schedule(id, place, "2026-11-20", true);
        var body = dayBody(0);
        body.put("durations", List.of(Map.of("stopId", place, "durationMinutes", 95)));
        putDay(id, "2026-11-20", body).andExpect(status().isBadRequest());
        body.put("expectedItineraryUpdatedAt", scheduleUpdatedAt(id).minusSeconds(1).toString());
        putDay(id, "2026-11-20", body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_010"));
        body.put("expectedItineraryUpdatedAt", scheduleUpdatedAt(id).toString());
        body.put("durations", List.of(Map.of("stopId", UUID.randomUUID(), "durationMinutes", 95)));
        putDay(id, "2026-11-20", body).andExpect(status().isConflict());
        body.put("durations", List.of(Map.of("stopId", place, "durationMinutes", 95), Map.of("stopId", place, "durationMinutes", 100)));
        putDay(id, "2026-11-20", body).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT start_location FROM trip_days WHERE trip_id=? AND date='2026-11-20'", String.class, UUID.fromString(id))).isNull();
        assertThat(jdbc.queryForObject("SELECT version FROM trips WHERE id=?", Long.class, UUID.fromString(id))).isZero();
    }
    @Test void dayEditsEnforceOwnershipAuthenticationCancellationAndVersions() throws Exception {
        String id = create().path("id").asText();
        mvc.perform(put("/api/v1/trips/"+id+"/days/2026-11-20").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(dayBody(0)))).andExpect(status().isUnauthorized());
        putDay(UUID.randomUUID().toString(), "2026-11-20", dayBody(0)).andExpect(status().isNotFound());
        putDay(id, "2026-11-20", dayBody(0)).andExpect(status().isOk());
        putDay(id, "2026-11-21", dayBody(0)).andExpect(status().isConflict());
        jdbc.update("UPDATE trips SET status='CANCELLED' WHERE id=?", UUID.fromString(id));
        putDay(id, "2026-11-20", dayBody(1)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_013"));
        var other = users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.com").fullName("Other")
                .passwordHash(encoder.encode("fixture-password")).build());
        jdbc.update("UPDATE trips SET user_id=? WHERE id=?", other.getId(), UUID.fromString(id));
        putDay(id, "2026-11-20", dayBody(1)).andExpect(status().isNotFound());
    }
    @Test void durationWriteFailureRollsBackDaySettingsAndVersion() throws Exception {
        String id = create().path("id").asText(); UUID place = place(); UUID item = schedule(id, place, "2026-11-20", true);
        var before = scheduleUpdatedAt(id);
        var body = dayBody(0); body.put("expectedItineraryUpdatedAt", before.toString());
        body.put("durations", List.of(Map.of("stopId", place, "durationMinutes", 95)));
        jdbc.execute("ALTER TABLE itinerary_items ADD CONSTRAINT day_duration_test_failure CHECK (id <> '"+item+"'::uuid OR duration_minutes <> 95)");
        try {
            putDay(id, "2026-11-20", body).andExpect(status().is5xxServerError());
            assertThat(jdbc.queryForObject("SELECT start_location FROM trip_days WHERE trip_id=? AND date='2026-11-20'", String.class, UUID.fromString(id))).isNull();
            assertThat(jdbc.queryForObject("SELECT version FROM trips WHERE id=?", Long.class, UUID.fromString(id))).isZero();
            assertThat(scheduleUpdatedAt(id)).isEqualTo(before);
        } finally { jdbc.execute("ALTER TABLE itinerary_items DROP CONSTRAINT day_duration_test_failure"); }
    }
    @Test void concurrentDayEditsWithSameVersionHaveOneWinner() throws Exception {
        String id = create().path("id").asText(); var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> first = () -> { assertThat(start.await(5,TimeUnit.SECONDS)).isTrue(); return putDay(id,"2026-11-20",dayBody(0)).andReturn().getResponse().getStatus(); };
            Callable<Integer> second = () -> { assertThat(start.await(5,TimeUnit.SECONDS)).isTrue(); return putDay(id,"2026-11-21",dayBody(0)).andReturn().getResponse().getStatus(); };
            var a = pool.submit(first); var b = pool.submit(second); start.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
    }
    @Test void patchPreservesOmittedFieldsAndSupportsExplicitNull() throws Exception {
        var trip=create(); String id=trip.path("id").asText();
        Map<String,Object> body=new LinkedHashMap<>(); body.put("version",0); body.put("title","  Gia đình  "); body.put("budget",null); body.put("accommodation"," Hotel ");
        var changed=read(patchTrip(id,body).andExpect(status().isOk())).path("trip");
        assertThat(changed.path("title").asText()).isEqualTo("Gia đình");
        assertThat(changed.path("budget").isNull()).isTrue();
        assertThat(changed.path("notes").asText()).isEqualTo("Original");
        assertThat(changed.path("accommodation").asText()).isEqualTo("Hotel");
        assertThat(changed.path("version").asLong()).isEqualTo(1);
        assertThat(changed.path("days")).isEqualTo(trip.path("days"));
    }
    @Test void normalizedNoOpDoesNotIncrementVersionOrUpdatedAt() throws Exception {
        var trip=create();
        var unchanged=read(patchTrip(trip.path("id").asText(),Map.of("version",0,"title"," Đà Lạt ","preferences",List.of())).andExpect(status().isOk())).path("trip");
        assertThat(unchanged).isEqualTo(trip);
    }
    @Test void invalidOrUnknownFieldsNeverMutateTrip() throws Exception {
        var trip=create(); String id=trip.path("id").asText();
        for (Map<String,?> body : List.<Map<String,?>>of(
                Map.of("version",0),Map.of("version",0,"owner","fake"),Map.of("version",0,"title"," "),
                Map.of("version",0,"startDate","2026-11-22"), Map.of("version",0,"startDate","2026-11-22","endDate","2026-11-20"),
                Map.of("version",0,"startDate","2026-01-01","endDate","2027-01-01"),Map.of("version",0,"preferences",List.of("nature","nature")),
                Map.of("version",0.5,"title","Bad"),Map.of("version","0","title","Bad"),Map.of("version",-1,"title","Bad"),
                Map.of("version",0,"destination",Map.of("name","Test","ownerId",UUID.randomUUID())),Map.of("version",0,"budget",Map.of("amount",1.5,"currency","VND")))) {
            assertThat(patchTrip(id,body).andReturn().getResponse().getStatus()).as("invalid body: %s",body).isEqualTo(400);
        }
        Map<String,Object> body=new HashMap<>(); body.put("version",0); body.put("title",null);
        patchTrip(id,body).andExpect(status().isBadRequest());
        assertThat(read(mvc.perform(get("/api/v1/trips/"+id).header("Authorization","Bearer "+token))).path("trip")).isEqualTo(trip);
    }
    @Test void foreignOwnerCannotReadConflictOrMutate() throws Exception {
        String id=create().path("id").asText();
        jdbc.update("UPDATE trips SET user_id=? WHERE id=?",users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.com").fullName("Other").build()).getId(),UUID.fromString(id));
        patchTrip(id,Map.of("version",99,"title","No")).andExpect(status().isNotFound());
        preview(id,99,"2026-11-21","2026-11-22").andExpect(status().isNotFound());
        deleteTrip(id,99).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/trips/"+id).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"title\":\"No\"}")).andExpect(status().isUnauthorized());
    }
    @Test void cancelledTripIsReadOnlyButDeletable() throws Exception {
        String id=create().path("id").asText(); jdbc.update("UPDATE trips SET status='CANCELLED' WHERE id=?",UUID.fromString(id));
        patchTrip(id,Map.of("version",0,"title","No")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_013"));
        preview(id,0,"2026-11-21","2026-11-22").andExpect(status().isConflict());
        deleteTrip(id,0).andExpect(status().isNoContent());
    }
    @Test void previewAndAcknowledgedChangePreserveOverlappingDayAndAllLegacyStopData() throws Exception {
        var trip=create(); String id=trip.path("id").asText(); UUID place=place(), item=schedule(id,place,"2026-11-20",true);
        UUID day=UUID.fromString(trip.path("days").get(1).path("id").asText());
        jdbc.update("UPDATE trip_days SET start_location='Hotel',return_to_start=true WHERE id=?",day);
        preview(id,0,"2026-11-21","2026-11-23").andExpect(status().isOk()).andExpect(jsonPath("$.data.movedToPoolCount").value(1))
                .andExpect(jsonPath("$.data.requiresAcknowledgement").value(true));
        var body=new HashMap<String,Object>(Map.of("version",0,"startDate","2026-11-21","endDate","2026-11-23"));
        patchTrip(id,body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_011"));
        assertThat(jdbc.queryForObject("SELECT removed_at FROM itinerary_items WHERE id=?",java.sql.Timestamp.class,item)).isNull();
        body.put("acknowledgeDateImpact",true);
        var changed=read(patchTrip(id,body).andExpect(status().isOk())).path("trip");
        assertThat(changed.path("days").get(0).path("id").asText()).isEqualTo(day.toString());
        assertThat(changed.path("readiness").path("unscheduledCount").asLong()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT start_location FROM trip_days WHERE id=?",String.class,day)).isEqualTo("Hotel");
        assertThat(jdbc.queryForObject("SELECT note FROM itinerary_items WHERE id=? AND removed_at IS NOT NULL",String.class,item)).isEqualTo("Appointment note");
        assertThat(jdbc.queryForObject("SELECT estimated_duration FROM trip_places WHERE trip_id=? AND place_id=?",Integer.class,UUID.fromString(id),place)).isEqualTo(75);
        assertThat(jdbc.queryForObject("SELECT notes FROM trip_places WHERE trip_id=? AND place_id=?",String.class,UUID.fromString(id),place)).isEqualTo("Place note");
        assertThat(jdbc.queryForObject("SELECT visited FROM trip_places WHERE trip_id=? AND place_id=?",Boolean.class,UUID.fromString(id),place)).isTrue();
        assertThat(jdbc.queryForObject("SELECT end_time FROM itinerary_items WHERE id=?",java.sql.Time.class,item).toString()).isEqualTo("10:30:00");
        assertThat(jdbc.queryForObject("SELECT return_to_start FROM trip_days WHERE id=?",Boolean.class,day)).isTrue();
        mvc.perform(get("/api/v1/trips/"+id+"/itinerary").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.days").isEmpty());
    }
    @Test void aPlaceStillScheduledOnAKeptDateDoesNotRequirePoolAcknowledgement() throws Exception {
        String id=create().path("id").asText(); UUID place=place(); schedule(id,place,"2026-11-20",true); schedule(id,place,"2026-11-21",true);
        preview(id,0,"2026-11-21","2026-11-22").andExpect(status().isOk()).andExpect(jsonPath("$.data.movedToPoolCount").value(0));
        patchTrip(id,Map.of("version",0,"startDate","2026-11-21","endDate","2026-11-22")).andExpect(status().isOk()).andExpect(jsonPath("$.data.trip.readiness.unscheduledCount").value(0));
    }
    @Test void legacyScheduleWithoutMembershipIsRecoveredIntoPool() throws Exception {
        String id=create().path("id").asText(); schedule(id,place(),"2026-11-20",false);
        patchTrip(id,Map.of("version",0,"startDate","2026-11-21","endDate","2026-11-22","acknowledgeDateImpact",true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.trip.stopCount").value(1)).andExpect(jsonPath("$.data.trip.readiness.unscheduledCount").value(1));
    }
    @Test void stalePreviewAndEditsRequireReloadRatherThanOverwriting() throws Exception {
        String id=create().path("id").asText();
        patchTrip(id,Map.of("version",0,"notes","Changed")).andExpect(status().isOk());
        patchTrip(id,Map.of("version",0,"title","No")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_010"));
        preview(id,0,"2026-11-21","2026-11-22").andExpect(status().isConflict());
        deleteTrip(id,0).andExpect(status().isConflict());
    }
    @Test void concurrentPatchesWithSameVersionHaveOneWinner() throws Exception {
        String id=create().path("id").asText(); var start=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> a=() -> { assertThat(start.await(5,TimeUnit.SECONDS)).isTrue(); return patchTrip(id,Map.of("version",0,"title","A")).andReturn().getResponse().getStatus(); };
            Callable<Integer> b=() -> { assertThat(start.await(5,TimeUnit.SECONDS)).isTrue(); return patchTrip(id,Map.of("version",0,"title","B")).andReturn().getResponse().getStatus(); };
            var first=pool.submit(a); var second=pool.submit(b); start.countDown();
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
    }
    @Test void deleteIsIdempotentAndSoftDeletesOnlyRelatedMemoriesWhileKeepingMedia() throws Exception {
        String id=create().path("id").asText(); UUID memory=UUID.randomUUID(), other=UUID.randomUUID();
        jdbc.update("INSERT INTO memories(id,user_id,trip_id,note) VALUES (?,?,?,'Keep metadata')",memory,owner,UUID.fromString(id));
        jdbc.update("INSERT INTO memories(id,user_id,note) VALUES (?,?,'Other memory')",other,owner);
        jdbc.update("INSERT INTO media(memory_id,url) VALUES (?,'https://example.com/fixture.jpg')",memory);
        deleteTrip(id,0).andExpect(status().isNoContent()); deleteTrip(id,0).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/trips/"+id).header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL FROM memories WHERE id=?",Boolean.class,memory)).isTrue();
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL FROM memories WHERE id=?",Boolean.class,other)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media WHERE memory_id=?",Integer.class,memory)).isEqualTo(1);
        mvc.perform(delete("/api/v1/trips/"+id).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        deleteTrip(id,-1).andExpect(status().isBadRequest());
    }
    @Test void dayInsertionFailureRollsBackUnschedulingAndMetadataTogether() throws Exception {
        String id=create().path("id").asText(); UUID item=schedule(id,place(),"2026-11-20",true);
        jdbc.execute("ALTER TABLE trip_days ADD CONSTRAINT phase2_test_failure CHECK (trip_id <> '"+id+"'::uuid OR date <> '2026-11-23'::date)");
        try {
            patchTrip(id,Map.of("version",0,"startDate","2026-11-21","endDate","2026-11-23","acknowledgeDateImpact",true)).andExpect(status().is5xxServerError());
            assertThat(jdbc.queryForObject("SELECT removed_at FROM itinerary_items WHERE id=?",java.sql.Timestamp.class,item)).isNull();
            assertThat(jdbc.queryForObject("SELECT version FROM trips WHERE id=?",Long.class,UUID.fromString(id))).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_days WHERE trip_id=?",Integer.class,UUID.fromString(id))).isEqualTo(3);
        } finally { jdbc.execute("ALTER TABLE trip_days DROP CONSTRAINT phase2_test_failure"); }
    }
    @Test void memoryDeletionFailureRollsBackTripDeletion() throws Exception {
        String id=create().path("id").asText(); UUID memory=UUID.randomUUID();
        jdbc.update("INSERT INTO memories(id,user_id,trip_id) VALUES (?,?,?)",memory,owner,UUID.fromString(id));
        jdbc.execute("ALTER TABLE memories ADD CONSTRAINT phase2_delete_failure CHECK (id <> '"+memory+"'::uuid OR deleted_at IS NULL)");
        try {
            deleteTrip(id,0).andExpect(status().is5xxServerError());
            mvc.perform(get("/api/v1/trips/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk());
            assertThat(jdbc.queryForObject("SELECT version FROM trips WHERE id=?",Long.class,UUID.fromString(id))).isZero();
            assertThat(jdbc.queryForObject("SELECT deleted_at FROM memories WHERE id=?",java.sql.Timestamp.class,memory)).isNull();
        } finally { jdbc.execute("ALTER TABLE memories DROP CONSTRAINT phase2_delete_failure"); }
    }
}
