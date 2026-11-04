package com.travel.trip;

import com.fasterxml.jackson.databind.*;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TripCrudIntegrationTest.FixedTime.class})
class TripCrudIntegrationTest {
    @TestConfiguration(proxyBeanMethods=false)
    static class FixedTime {
        @Bean @Primary Clock tripTestClock() { return Clock.fixed(Instant.parse("2026-11-02T03:00:00Z"),ZoneOffset.UTC); }
    }
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean AuthEmailSender mail;
    MockMvc mvc;
    String token;
    UUID owner;
    @BeforeEach void setup() throws Exception {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String password=UUID.randomUUID()+".Ab1";
        User user=users.save(User.builder().fullName("Trip Tester").email(UUID.randomUUID()+"@example.com")
            .passwordHash(encoder.encode(password)).emailVerifiedAt(Instant.parse("2026-11-01T00:00:00Z")).build());
        owner=user.getId();
        token=json.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",user.getEmail(),"password",password))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }
    Map<String,Object> request() {
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("clientRequestId",UUID.randomUUID()); r.put("destination",Map.of("name","Đà Lạt"));
        r.put("startDate","2026-11-20"); r.put("endDate","2026-11-22");
        r.put("budget",Map.of("amount",0,"currency","VND")); r.put("preferences",List.of("nature"));
        return r;
    }
    ResultActions create(Map<String,Object> r) throws Exception {
        return mvc.perform(post("/api/v1/trips").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(r)));
    }
    JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }
    @Test void createListAndReadPersistedTripThroughRealAuth() throws Exception {
        JsonNode created=data(create(request()).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.trip.status").value("PLANNING"))
            .andExpect(jsonPath("$.data.trip.mode").value("PLANNING"))
            .andExpect(jsonPath("$.data.trip.title").value("Đà Lạt"))
            .andExpect(jsonPath("$.data.trip.budget.amount").value(0))
            .andExpect(jsonPath("$.data.trip.days.length()").value(3))
            .andExpect(jsonPath("$.data.trip.user").doesNotExist())
            .andExpect(jsonPath("$.data.trip.createRequestHash").doesNotExist()));
        String id=created.path("trip").path("id").asText();
        JsonNode fetched=data(mvc.perform(get("/api/v1/trips/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk()));
        assertThat(fetched.path("trip")).isEqualTo(created.path("trip"));
        mvc.perform(get("/api/v1/trips").param("mode","PLANNING").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.content[0].id").value(id));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itineraries WHERE trip_id=?",Long.class,UUID.fromString(id))).isEqualTo(1);
        mvc.perform(get("/api/v1/trips/"+id)).andExpect(status().isUnauthorized());
    }
    @Test void replayAndConflictingRequestKeys() throws Exception {
        var body=request(); String id=data(create(body).andExpect(status().isCreated())).path("trip").path("id").asText();
        create(body).andExpect(status().isOk()).andExpect(jsonPath("$.data.trip.id").value(id));
        body.put("title","Another trip");
        create(body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_009"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips WHERE user_id=?",Long.class,owner)).isEqualTo(1);
    }
    @Test void simultaneousReplayCreatesOneTripAndOneSetOfDays() throws Exception {
        var body=request(); CountDownLatch start=new CountDownLatch(1);
        try (ExecutorService pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> action=() -> { assertThat(start.await(5,TimeUnit.SECONDS)).isTrue(); return create(body).andReturn().getResponse().getStatus(); };
            Future<Integer> a=pool.submit(action),b=pool.submit(action); start.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,200);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips WHERE user_id=?",Long.class,owner)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_days d JOIN trips t ON t.id=d.trip_id WHERE t.user_id=?",Long.class,owner)).isEqualTo(3);
    }
    @Test void validationAndReferencesNeverPartiallyCreate() throws Exception {
        var body=request(); body.put("initialPlaceIds",List.of(UUID.randomUUID()));
        create(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TRIP_012"));
        body=request(); body.put("startDate","2026-11-01");
        create(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.startDate").exists());
        body=request(); body.put("preferences",List.of("nature","nature"));
        create(body).andExpect(status().isBadRequest());
        body=request(); body.put("preferences",List.of(0)); create(body).andExpect(status().isBadRequest());
        body=request(); body.put("status","ACTIVE"); create(body).andExpect(status().isBadRequest());
        body=request(); body.put("isDraft",true); create(body).andExpect(status().isBadRequest());
        for (String field : List.of("title","timeZone","preferences","initialPlaceIds")) {
            body=request(); body.put(field,null); create(body).andExpect(status().isBadRequest());
        }
        body=request(); body.put("endDate","2028-11-22"); create(body).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trips WHERE user_id=?",Long.class,owner)).isZero();
    }
    @Test void otherOwnerAndDeletedTripsAreNotVisible() throws Exception {
        String id=data(create(request()).andExpect(status().isCreated())).path("trip").path("id").asText();
        UUID another=users.save(User.builder().fullName("Other").email(UUID.randomUUID()+"@example.com").build()).getId();
        jdbc.update("UPDATE trips SET user_id=? WHERE id=?",another,UUID.fromString(id));
        mvc.perform(get("/api/v1/trips/"+id).header("Authorization","Bearer "+token))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TRIP_001"));
        mvc.perform(get("/api/v1/trips").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        jdbc.update("UPDATE trips SET user_id=?,deleted_at=now() WHERE id=?",owner,UUID.fromString(id));
        mvc.perform(get("/api/v1/trips/"+id).header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
    }
    @Test void pagingAndLiteralSearchAreBounded() throws Exception {
        var r=request(); r.put("title","A 100% trip"); create(r).andExpect(status().isCreated());
        create(request()).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/trips").param("q","%").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get("/api/v1/trips").param("size","1").param("page","1").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalPages").value(2));
        mvc.perform(get("/api/v1/trips").param("size","101").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/trips").param("sort","userId,desc").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
    }

    @Test void childWriteFailureRollsBackTripAndItinerary() throws Exception {
        jdbc.execute("ALTER TABLE trip_days ADD CONSTRAINT test_reject_trip_day CHECK (date <> DATE '2026-11-21') NOT VALID");
        try {
            create(request()).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM trips WHERE user_id=?",Long.class,owner)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM itineraries i LEFT JOIN trips t ON t.id=i.trip_id WHERE t.id IS NULL",Long.class)).isZero();
        } finally {
            jdbc.execute("ALTER TABLE trip_days DROP CONSTRAINT test_reject_trip_day");
        }
    }

    @Test void oneYearBoundaryAndNullableFieldsMatchReadContract() throws Exception {
        var r=request(); r.remove("budget"); r.put("startDate","2026-11-02"); r.put("endDate","2027-11-01");
        create(r).andExpect(status().isCreated()).andExpect(jsonPath("$.data.trip.dayCount").value(365))
            .andExpect(jsonPath("$.data.trip.days.length()").value(365))
            .andExpect(jsonPath("$.data.trip.mode").value("LIVE"))
            .andExpect(jsonPath("$.data.trip.coverImageUrl").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.trip.budget").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.trip.destination.provinceId").value(org.hamcrest.Matchers.nullValue()));
        r.put("clientRequestId",UUID.randomUUID()); r.put("endDate","2027-11-02");
        create(r).andExpect(status().isBadRequest());
    }
}
