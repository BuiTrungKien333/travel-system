package com.travel.home;

import com.fasterxml.jackson.databind.*;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class,HomeDashboardIntegrationTest.FixedTime.class})
class HomeDashboardIntegrationTest {
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.travel.maps.service.MapboxDirectionsService directions;
    static final LocalDate TODAY=LocalDate.of(2026,11,7);
    @TestConfiguration(proxyBeanMethods=false)
    static class FixedTime { @Bean @Primary Clock homeClock() { return Clock.fixed(Instant.parse("2026-11-06T17:30:00Z"),ZoneOffset.UTC); } }
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired com.travel.weather.repository.TripWeatherRepository weatherLocations;
    @MockitoBean AuthEmailSender sender;
    MockMvc mvc; UUID owner, stranger; String token;
    @BeforeEach void setup() throws Exception {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var user=users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.com").fullName("Home QA")
                .passwordHash(passwords.encode("fixture-password")).emailVerifiedAt(Instant.parse("2026-11-01T00:00:00Z")).build());
        owner=user.getId();
        stranger=users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.com").fullName("Other QA")
                .passwordHash(passwords.encode("fixture-password")).emailVerifiedAt(Instant.parse("2026-11-01T00:00:00Z")).build()).getId();
        token=data(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",user.getEmail(),"password","fixture-password")))).andExpect(status().isOk())).path("accessToken").asText();
    }
    JsonNode data(ResultActions result) throws Exception { return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data"); }
    JsonNode home() throws Exception { return data(mvc.perform(get("/api/v1/home/dashboard").header("Authorization","Bearer "+token)).andExpect(status().isOk())); }
    UUID trip(UUID user,LocalDate start,LocalDate end,String status,int created) {
        UUID id=UUID.randomUUID();
        jdbc.update("""
            INSERT INTO trips(id,user_id,name,destination,start_date,end_date,status,preferences,created_at,updated_at)
            VALUES (?,?,?,'Đà Nẵng',?,?,?,'[]',?::timestamptz,?::timestamptz)
            """,id,user,"Trip "+created,start,end,status,Instant.parse("2026-01-01T00:00:00Z").plusSeconds(created).toString(),Instant.parse("2026-01-01T00:00:00Z").plusSeconds(created).toString());
        if(start!=null && end!=null && !end.isBefore(start) && end.toEpochDay()-start.toEpochDay()<365)
            jdbc.update("INSERT INTO trip_days(trip_id,date) SELECT ?,day::date FROM generate_series(?::timestamp,?::timestamp,interval '1 day') day",id,start,end);
        return id;
    }
    UUID province() { UUID id=UUID.randomUUID();jdbc.update("INSERT INTO provinces(id,code,name) VALUES (?,?,?)",id,UUID.randomUUID().toString().substring(0,8),"Province QA");return id; }
    UUID place(UUID province) { UUID id=UUID.randomUUID();jdbc.update("INSERT INTO places(id,name,address,latitude,longitude,province_id) VALUES (?,'Home stop','Address',16,108,?)",id,province);return id; }
    void link(UUID trip,UUID place,boolean visited) { jdbc.update("INSERT INTO trip_places(trip_id,place_id,visited) VALUES (?,?,?)",trip,place,visited); }
    UUID itinerary(UUID trip) { UUID id=UUID.randomUUID();jdbc.update("INSERT INTO itineraries(id,trip_id) VALUES (?,?)",id,trip);return id; }
    void stop(UUID itinerary,UUID place,LocalDate date,int sequence) {
        jdbc.update("""
            INSERT INTO itinerary_items(itinerary_id,place_id,date,arrival_at,duration_minutes,sequence)
            VALUES (?,?,?,?,45,?)
            """,itinerary,place,date,date.atTime(9+sequence,0),sequence);
    }
    UUID memory(UUID user,UUID trip,UUID place,Instant at) {
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO memories(id,user_id,trip_id,place_id,note,experienced_at) VALUES (?,?,?,?,'Real memory',?)",id,user,trip,place,java.sql.Timestamp.from(at));return id;
    }
    void photo(UUID memory,int order) { jdbc.update("INSERT INTO media(memory_id,url,mime_type,display_order) VALUES (?,'https://example.com/photo.webp','image/webp',?)",memory,order); }

    @Test void requiresAuthenticationAndNewIsNotAnErrorFallback() throws Exception {
        mvc.perform(get("/api/v1/home/dashboard")).andExpect(status().isUnauthorized());
        var result=home();
        assertThat(result.path("state").asText()).isEqualTo("NEW");
        assertThat(result.path("selectedTrip").isNull()).isTrue();
        assertThat(result.path("context").path("evaluatedDate").asText()).isEqualTo(TODAY.toString());
        assertThat(result.path("recentPhotos").size()).isZero();
        trip(owner,TODAY.minusDays(4),TODAY.minusDays(2),"CANCELLED",9);
        assertThat(home().path("state").asText()).isEqualTo("NEW");
    }

    @Test void batchAddAppendsToSelectedDayAndRetryDoesNotDuplicateStops() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY.plusDays(1), "ACTIVE", 1);
        UUID province = province();
        UUID first = place(province), otherDay = place(province), added = place(province);
        link(trip, first, false);
        link(trip, otherDay, false);
        UUID schedule = itinerary(trip);
        stop(schedule, first, TODAY, 0);
        stop(schedule, otherDay, TODAY.plusDays(1), 0);
        var body = json.writeValueAsString(Map.of("placeIds", List.of(first, added), "date", TODAY));
        String path = "/api/v1/trips/{id}/places/batch";

        mvc.perform(post(path, trip).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        var after = data(mvc.perform(get("/api/v1/trips/{id}/itinerary", trip)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        assertThat(after.path("days").path(0).path("stops").size()).isEqualTo(2);
        var existing = after.path("days").path(0).path("stops").path(0);
        var appended = after.path("days").path(0).path("stops").path(1);
        assertThat(existing.path("placeId").asText()).isEqualTo(first.toString());
        assertThat(existing.path("arrivalTime").asText()).isEqualTo(TODAY + "T09:00:00");
        assertThat(appended.path("placeId").asText()).isEqualTo(added.toString());
        assertThat(appended.path("arrivalTime").isNull()).isTrue();
        assertThat(appended.path("durationMinutes").asInt()).isEqualTo(45);
        assertThat(appended.path("sequence").asInt()).isEqualTo(1);
        assertThat(after.path("days").path(1).path("stops").path(0).path("placeId").asText()).isEqualTo(otherDay.toString());

        mvc.perform(post(path, trip).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itinerary_items WHERE itinerary_id=?", Integer.class, schedule)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip)).isEqualTo(3);
    }

    @Test void batchAddCreatesTheSelectedDayScheduleWithoutAnUnscheduledStep() throws Exception {
        UUID trip = trip(owner, TODAY.plusDays(1), TODAY.plusDays(2), "PLANNING", 1);
        UUID selected = place(province());
        mvc.perform(post("/api/v1/trips/{id}/places/batch", trip)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("placeIds", List.of(selected), "date", TODAY.plusDays(2)))))
                .andExpect(status().isOk());
        var schedule = data(mvc.perform(get("/api/v1/trips/{id}/itinerary", trip)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        assertThat(schedule.path("days").size()).isEqualTo(1);
        assertThat(schedule.path("days").path(0).path("date").asText()).isEqualTo(TODAY.plusDays(2).toString());
        assertThat(schedule.path("days").path(0).path("stops").path(0).path("placeId").asText()).isEqualTo(selected.toString());
        assertThat(schedule.path("updatedAt").isTextual()).isTrue();
    }

    @Test void invalidDayOrPastLiveDayRollsBackBothMembershipsAndSchedule() throws Exception {
        UUID trip = trip(owner, TODAY.minusDays(1), TODAY.plusDays(1), "ACTIVE", 1);
        UUID selected = place(province());
        for (LocalDate date : List.of(TODAY.minusDays(1), TODAY.plusDays(2))) {
            mvc.perform(post("/api/v1/trips/{id}/places/batch", trip)
                    .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("placeIds", List.of(selected), "date", date))))
                    .andExpect(status().is(date.isBefore(TODAY) ? 409 : 400))
                    .andExpect(jsonPath("$.code").value(date.isBefore(TODAY) ? "TRIP_006" : "TRIP_005"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Integer.class, trip)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itineraries WHERE trip_id=?", Integer.class, trip)).isZero();
        UUID foreign = trip(stranger, TODAY, TODAY.plusDays(1), "ACTIVE", 2);
        mvc.perform(post("/api/v1/trips/{id}/places/batch", foreign)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("placeIds", List.of(selected), "date", TODAY))))
                .andExpect(status().isNotFound());
    }

    @Test void dayRouteUsesOwnedSavedWaypointsInScheduleOrder() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY.plusDays(1), "PLANNED", 1);
        UUID foreign = trip(stranger, TODAY, TODAY, "PLANNED", 2);
        UUID province = province();
        UUID first = place(province);
        UUID second = place(province);
        UUID removed = place(province);
        UUID schedule = itinerary(trip);
        link(trip, first, false);
        link(trip, second, false);
        link(trip, removed, false);
        stop(schedule, second, TODAY, 1);
        stop(schedule, first, TODAY, 0);
        stop(schedule, removed, TODAY, 2);
        jdbc.update("UPDATE itinerary_items SET removed_at=now() WHERE itinerary_id=? AND place_id=?", schedule, removed);
        String routePath = "/api/v1/trips/{id}/itinerary/days/{date}/route";
        mvc.perform(get(routePath, trip, TODAY)).andExpect(status().isUnauthorized());
        mvc.perform(get(routePath, foreign, TODAY).header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
        mvc.perform(get(routePath, trip, TODAY.minusDays(1)).header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        var response = data(mvc.perform(get(routePath, trip, TODAY).header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        assertThat(response.path("status").asText()).isEqualTo("NOT_CONFIGURED");
        assertThat(response.path("placeIds").path(0).asText()).isEqualTo(first.toString());
        assertThat(response.path("placeIds").path(1).asText()).isEqualTo(second.toString());
        assertThat(response.path("placeIds").size()).isEqualTo(2);
        mvc.perform(get(routePath, trip, TODAY.plusDays(1)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("TOO_FEW_STOPS"));
    }

    @Test void tripJournalFiltersByVietnamDayAndOwnershipBeforePaging() throws Exception {
        UUID trip = trip(owner, TODAY.minusDays(1), TODAY, "COMPLETED", 1);
        UUID foreign = trip(stranger, TODAY, TODAY, "COMPLETED", 2);
        memory(owner, trip, null, Instant.parse("2026-11-06T16:59:59Z"));
        UUID first = memory(owner, trip, null, Instant.parse("2026-11-06T17:00:00Z"));
        photo(first, 0);
        UUID second = memory(owner, trip, null, Instant.parse("2026-11-07T00:00:00Z"));
        memory(stranger, trip, null, Instant.parse("2026-11-07T01:00:00Z"));
        memory(owner, trip, null, Instant.parse("2026-11-07T17:00:00Z"));
        String path = "/api/v1/journal/memories";
        var page = data(mvc.perform(get(path).header("Authorization", "Bearer " + token)
                .param("tripId", trip.toString()).param("date", TODAY.toString()).param("size", "1"))
                .andExpect(status().isOk()));
        assertThat(page.path("totalElements").asInt()).isEqualTo(2);
        assertThat(page.path("content").path(0).path("id").asText()).isEqualTo(second.toString());
        mvc.perform(get(path).header("Authorization", "Bearer " + token)
                .param("tripId", trip.toString()).param("date", TODAY.toString()).param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].id").value(first.toString()))
                .andExpect(jsonPath("$.data.content[0].media[0].url").value("https://example.com/photo.webp"));
        mvc.perform(get(path).header("Authorization", "Bearer " + token)
                .param("tripId", foreign.toString()).param("date", TODAY.toString())).andExpect(status().isNotFound());
    }
    @Test void livePriorityAndNewestCreationAreGlobalNotLimitedToTwentyTrips() throws Exception {
        UUID older=trip(owner,TODAY.minusDays(1),TODAY.plusDays(1),"ACTIVE",1);
        UUID chosen=trip(owner,TODAY,TODAY,"PLANNING",2);
        jdbc.update("UPDATE trips SET updated_at=now() WHERE id=?",older);
        for(int i=0;i<25;i++) trip(owner,TODAY.minusDays(10),TODAY.minusDays(8),"COMPLETED",100+i);
        trip(owner,TODAY.plusDays(1),TODAY.plusDays(2),"PLANNING",200);
        trip(stranger,TODAY,TODAY,"ACTIVE",300);
        UUID deleted=trip(owner,TODAY,TODAY,"ACTIVE",400);
        jdbc.update("UPDATE trips SET deleted_at=now() WHERE id=?",deleted);
        var result=home();
        assertThat(result.path("state").asText()).isEqualTo("LIVE");
        assertThat(result.path("selectedTrip").path("trip").path("id").asText()).isEqualTo(chosen.toString());
        assertThat(result.path("dayIndex").asInt()).isZero();
    }
    @Test void upcomingOutranksDistantPlanningButNewestWinsInsideEachState() throws Exception {
        trip(owner,TODAY.plusDays(8),TODAY.plusDays(10),"PLANNING",100);
        trip(owner,TODAY.plusDays(1),TODAY.plusDays(2),"PLANNING",1);
        UUID chosen=trip(owner,TODAY.plusDays(7),TODAY.plusDays(8),"READY",2);
        var result=home();
        assertThat(result.path("state").asText()).isEqualTo("UPCOMING");
        assertThat(result.path("selectedTrip").path("trip").path("id").asText()).isEqualTo(chosen.toString());
        jdbc.update("UPDATE trips SET status='CANCELLED' WHERE user_id=? AND start_date<=?",owner,TODAY.plusDays(7));
        assertThat(home().path("state").asText()).isEqualTo("PLANNING");
    }
    @Test void latestPlanningTripIsSelectedEvenIfItDepartsLater() throws Exception {
        trip(owner,TODAY.plusDays(8),TODAY.plusDays(9),"PLANNING",1);
        UUID chosen=trip(owner,TODAY.plusDays(40),TODAY.plusDays(42),"PLANNING",2);
        var result=home();
        assertThat(result.path("state").asText()).isEqualTo("PLANNING");
        assertThat(result.path("selectedTrip").path("trip").path("id").asText()).isEqualTo(chosen.toString());
        assertThat(result.path("firstEmptyDay").asInt()).isEqualTo(1);
    }
    @Test void pastTripsUseReturningAndUndatedTripsRemainPlanning() throws Exception {
        trip(owner,TODAY.minusDays(5),TODAY.minusDays(2),"PLANNING",1);
        UUID completed=trip(owner,TODAY.plusDays(5),TODAY.plusDays(6),"COMPLETED",2);
        assertThat(home().path("state").asText()).isEqualTo("RETURNING");
        assertThat(home().path("selectedTrip").path("trip").path("id").asText()).isEqualTo(completed.toString());
        UUID undated=trip(owner,null,null,"PLANNING",3);
        assertThat(home().path("state").asText()).isEqualTo("PLANNING");
        assertThat(home().path("selectedTrip").path("trip").path("id").asText()).isEqualTo(undated.toString());
    }
    @Test void nextStopUsesTodayScheduleAndPersistedVisitedFlags() throws Exception {
        UUID trip=trip(owner,TODAY,TODAY.plusDays(1),"ACTIVE",1), itinerary=itinerary(trip);
        UUID province=province(),first=place(province),second=place(province),third=place(province),tomorrow=place(province);
        for(UUID p:List.of(first,second,third,tomorrow)) link(trip,p,p.equals(first));
        stop(itinerary,first,TODAY,0);stop(itinerary,second,TODAY,1);stop(itinerary,third,TODAY,2);stop(itinerary,tomorrow,TODAY.plusDays(1),0);
        var result=home();
        assertThat(result.path("todayStopCount").asInt()).isEqualTo(3);
        assertThat(result.path("nextStop").path("placeId").asText()).isEqualTo(second.toString());
        mvc.perform(put("/api/v1/trips/{trip}/places/{place}",trip,second).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"visited\":true}")).andExpect(status().isOk());
        assertThat(home().path("nextStop").path("placeId").asText()).isEqualTo(third.toString());
        jdbc.update("UPDATE trip_places SET visited=true WHERE trip_id=?",trip);
        assertThat(home().path("nextStop").isNull()).isTrue();
    }

    @Test void liveCoverIsReturnedByHomeDetailAndList() throws Exception {
        UUID id=trip(owner,TODAY,TODAY.plusDays(2),"ACTIVE",1);
        String cover="https://example.com/trip-cover.jpg";
        jdbc.update("UPDATE trips SET cover_image=? WHERE id=?",cover,id);
        assertThat(home().path("selectedTrip").path("trip").path("coverImageUrl").asText()).isEqualTo(cover);
        mvc.perform(get("/api/v1/trips/{id}",id).header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.trip.coverImageUrl").value(cover));
        mvc.perform(get("/api/v1/trips").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].coverImageUrl").value(cover));
    }

    @Test void liveVisitAndUndoAdvanceHomeAndRejectForeignWrites() throws Exception {
        UUID id=trip(owner,TODAY,TODAY.plusDays(1),"ACTIVE",1), schedule=itinerary(id);
        UUID first=place(province()),second=place(province());
        link(id,first,false);link(id,second,false);stop(schedule,first,TODAY,0);stop(schedule,second,TODAY,1);
        mvc.perform(put("/api/v1/trips/{trip}/places/{place}",id,first).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"visited\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visited").value(true));
        assertThat(home().path("nextStop").path("placeId").asText()).isEqualTo(second.toString());
        mvc.perform(put("/api/v1/trips/{trip}/places/{place}",id,first).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"visited\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.visited").value(false));
        assertThat(home().path("nextStop").path("placeId").asText()).isEqualTo(first.toString());
        UUID foreign=trip(stranger,TODAY,TODAY.plusDays(1),"ACTIVE",2);link(foreign,first,false);
        mvc.perform(put("/api/v1/trips/{trip}/places/{place}",foreign,first).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"visited\":true}")).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT visited FROM trip_places WHERE trip_id=? AND place_id=?",Boolean.class,foreign,first)).isFalse();
        jdbc.update("UPDATE trips SET status='CANCELLED' WHERE id=?",id);
        mvc.perform(put("/api/v1/trips/{trip}/places/{place}",id,first).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"visited\":true}")).andExpect(status().isConflict());
    }
    @Test void completionUsesSavedScheduleNotMerelyTripMemberships() throws Exception {
        UUID trip=trip(owner,TODAY.plusDays(3),TODAY.plusDays(5),"PLANNING",1), schedule=itinerary(trip);
        UUID p=place(province());link(trip,p,false);stop(schedule,p,TODAY.plusDays(3),0);
        assertThat(home().path("firstEmptyDay").asInt()).isEqualTo(2);
        jdbc.update("UPDATE itinerary_items SET removed_at=now() WHERE itinerary_id=?",schedule);
        assertThat(home().path("firstEmptyDay").asInt()).isEqualTo(1);
    }
    @Test void footprintsCountUniqueMemoryPlacesAndProvincesNotPlannedStops() throws Exception {
        UUID trip=trip(owner,TODAY.minusDays(3),TODAY.minusDays(1),"COMPLETED",1);
        UUID province=province(),p1=place(province),p2=place(province),planned=place(province());
        link(trip,p1,true);link(trip,p2,true);link(trip,planned,false);
        Instant at=Instant.parse("2026-11-06T18:00:00Z");
        memory(owner,trip,p1,at);memory(owner,trip,p1,at);memory(owner,trip,p2,at);
        memory(stranger,null,planned,at);
        UUID removed=memory(owner,trip,planned,at);jdbc.update("UPDATE memories SET deleted_at=now() WHERE id=?",removed);
        UUID foreignTrip=trip(stranger,TODAY,TODAY,"ACTIVE",3);memory(owner,foreignTrip,planned,at);
        var stats=home().path("footprint");
        assertThat(stats.path("provinces").asInt()).isEqualTo(1);
        assertThat(stats.path("places").asInt()).isEqualTo(2);
        assertThat(stats.path("hasHistory").asBoolean()).isTrue();
        jdbc.update("UPDATE trips SET deleted_at=now() WHERE id=?",trip);
        assertThat(home().path("footprint").path("places").asInt()).isZero();
    }
    @Test void todayPhotosAreBoundedAndUseVietnamDayAndOwnerScope() throws Exception {
        UUID trip=trip(owner,TODAY,TODAY,"ACTIVE",1);
        UUID yesterday=memory(owner,trip,null,Instant.parse("2026-11-06T16:59:00Z"));photo(yesterday,0);
        for(int i=0;i<8;i++) photo(memory(owner,trip,null,Instant.parse("2026-11-06T17:01:00Z").plusSeconds(i*60)),0);
        photo(memory(stranger,trip,null,Instant.parse("2026-11-06T17:20:00Z")),0);
        UUID removed=memory(owner,trip,null,Instant.parse("2026-11-06T17:29:00Z"));photo(removed,0);jdbc.update("UPDATE memories SET deleted_at=now() WHERE id=?",removed);
        var photos=home().path("recentPhotos");
        assertThat(photos.size()).isEqualTo(6);
        for(var p:photos) { assertThat(p.path("date").asText()).isEqualTo(TODAY.toString());assertThat(p.path("tripId").asText()).isEqualTo(trip.toString()); }
        assertThat(photos.get(0).path("time").asText()).isEqualTo("00:08");
    }
    @Test void weatherEndpointEnforcesOwnerAndDoesNotForecastDistantOrDeletedTrips() throws Exception {
        UUID foreign=trip(stranger,TODAY,TODAY,"ACTIVE",1);
        mvc.perform(get("/api/v1/trips/"+foreign+"/weather")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/trips/"+foreign+"/weather").header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
        UUID distant=trip(owner,TODAY.plusDays(16),TODAY.plusDays(18),"PLANNING",2);
        mvc.perform(get("/api/v1/trips/"+distant+"/weather").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.unavailableReason").value("OUTSIDE_FORECAST_WINDOW"))
            .andExpect(jsonPath("$.data.forecast").isEmpty());
        jdbc.update("UPDATE trips SET deleted_at=now() WHERE id=?",distant);
        mvc.perform(get("/api/v1/trips/"+distant+"/weather").header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
    }
    @Test void weatherLocationUsesSavedPlaceAndNeverLeaksForeignPrivatePlace() throws Exception {
        UUID trip=trip(owner,TODAY,TODAY,"ACTIVE",1), province=province(), place=place(province);
        jdbc.update("UPDATE places SET source='USER',owner_id=? WHERE id=?",stranger,place);link(trip,place,false);
        assertThat(weatherLocations.location(owner,trip,null,TODAY)).isEmpty();
        mvc.perform(get("/api/v1/trips/"+trip+"/weather").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.unavailableReason").value("NO_LOCATION"));
        jdbc.update("UPDATE places SET owner_id=? WHERE id=?",owner,place);
        var selected=weatherLocations.location(owner,trip,null,TODAY).orElseThrow();
        assertThat(selected.placeId()).isEqualTo(place);assertThat(selected.latitude()).isEqualTo(16);
        mvc.perform(get("/api/v1/trips/"+trip+"/weather").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.location.placeId").value(place.toString()))
            .andExpect(jsonPath("$.data.unavailableReason").value("MOCK_PROVIDER"));
    }

    private ResultActions saveDay(UUID trip, LocalDate date, String revision, List<Map<String, Object>> stops) throws Exception {
        var body = json.createObjectNode();
        body.put("expectedTripVersion", 0);
        if (revision == null) body.putNull("expectedUpdatedAt");
        else body.put("expectedUpdatedAt", revision);
        body.set("stops", json.valueToTree(stops));
        return mvc.perform(put("/api/v1/trips/{id}/itinerary/days/{date}", trip, date)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private ResultActions daySchedule(UUID trip, LocalDate day, String revision, Object settings, List<Map<String, Object>> durations) throws Exception {
        var body = json.createObjectNode().put("expectedTripVersion", 0);
        if (revision == null) body.putNull("expectedUpdatedAt"); else body.put("expectedUpdatedAt", revision);
        body.set("settings", json.valueToTree(settings));
        body.set("durations", json.valueToTree(durations));
        return mvc.perform(put("/api/v1/trips/{id}/itinerary/days/{day}/schedule", trip, day)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private Map<String, Object> originSettings(boolean coordinates) {
        var origin = new HashMap<String, Object>();
        origin.put("name", "My lodging"); origin.put("address", "Private address");
        origin.put("latitude", coordinates ? 16.01 : null); origin.put("longitude", coordinates ? 108.01 : null);
        return Map.of("origin", origin, "departureTime", "08:30", "returnToStart", true, "travelMode", "DRIVE");
    }

    @Test void daySettingsAndDurationsAreAtomicAndCalculateArrivalTimesFromRealLegs() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY.plusDays(1), "ACTIVE", 1), province = province();
        UUID first = place(province), second = place(province), other = place(province);
        for (UUID place : List.of(first, second, other)) link(trip, place, false);
        UUID itinerary = itinerary(trip);
        stop(itinerary, first, TODAY, 0); stop(itinerary, second, TODAY, 1); stop(itinerary, other, TODAY.plusDays(1), 0);
        jdbc.update("UPDATE itinerary_items SET note='Keep note' WHERE itinerary_id=? AND place_id=?", itinerary, first);
        var before = data(mvc.perform(get("/api/v1/trips/{id}/itinerary", trip).header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        var route = new com.travel.maps.dto.TripDayRoute(com.travel.maps.dto.TripDayRoute.Status.READY,
                new com.travel.maps.dto.TripDayRoute.Route(5000, 2280, "_p~iF~ps|U_ulLnnqC", List.of(
                        new com.travel.maps.dto.TripDayRoute.Leg(1000, 600), new com.travel.maps.dto.TripDayRoute.Leg(3000, 1200),
                        new com.travel.maps.dto.TripDayRoute.Leg(1000, 480))));
        org.mockito.Mockito.doReturn(route).when(directions).route(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
        var saved = data(daySchedule(trip, TODAY, before.path("updatedAt").asText(), originSettings(true),
                List.of(Map.of("placeId", first, "minutes", 60), Map.of("placeId", second, "minutes", 45))).andExpect(status().isOk()));
        assertThat(saved.at("/days/0/settings/origin/name").asText()).isEqualTo("My lodging");
        assertThat(saved.at("/days/0/stops/0/arrivalTime").asText()).isEqualTo(TODAY + "T08:40:00");
        assertThat(saved.at("/days/0/stops/1/arrivalTime").asText()).isEqualTo(TODAY + "T10:00:00");
        assertThat(saved.at("/days/0/stops/0/note").asText()).isEqualTo("Keep note");
        assertThat(saved.at("/days/0/stops/0/id")).isEqualTo(before.at("/days/0/stops/0/id"));
        assertThat(saved.at("/days/1")).isEqualTo(before.at("/days/1"));
        mvc.perform(get("/api/v1/trips/{id}/itinerary/days/{day}/route", trip, TODAY).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.route.legs.length()").value(1))
                .andExpect(jsonPath("$.data.startLeg.durationSeconds").value(600))
                .andExpect(jsonPath("$.data.returnLeg.durationSeconds").value(480));
        daySchedule(trip, TODAY, before.path("updatedAt").asText(), originSettings(true),
                List.of(Map.of("placeId", first, "minutes", 30), Map.of("placeId", second, "minutes", 30)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_008"));
        saveDay(trip, TODAY, saved.path("updatedAt").asText(), List.of(scheduledStop(first, TODAY, 0, "Edited stop")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.days[0].settings.origin.name").value("My lodging"));
    }

    @Test void manualOriginIsSavedWithoutInventingCoordinatesOrArrivalTimes() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY, "ACTIVE", 1), place = place(province());
        link(trip, place, false); UUID itinerary = itinerary(trip); stop(itinerary, place, TODAY, 0);
        var before = data(mvc.perform(get("/api/v1/trips/{id}/itinerary", trip).header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        daySchedule(trip, TODAY, before.path("updatedAt").asText(), originSettings(false), List.of(Map.of("placeId", place, "minutes", 45)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.days[0].settings.origin.latitude").isEmpty())
                .andExpect(jsonPath("$.data.days[0].stops[0].arrivalTime").isEmpty());
    }

    @Test void optimizationUsesSavedOriginAndReturnsOnlyVisitIdsForARoundTrip() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY, "ACTIVE", 1), province = province(), itinerary = itinerary(trip);
        List<UUID> visits = List.of(place(province), place(province), place(province), place(province));
        for (int i = 0; i < visits.size(); i++) { link(trip, visits.get(i), false); stop(itinerary, visits.get(i), TODAY, i); }
        jdbc.update("INSERT INTO itinerary_day_settings(id,itinerary_id,day_date,origin_name,origin_latitude,origin_longitude,departure_time,return_to_start,travel_mode) VALUES (?,?,?,'Origin',16.01,108.01,'08:30',true,'DRIVE')",
                UUID.randomUUID(), itinerary, TODAY);
        org.mockito.Mockito.doReturn(new com.travel.maps.service.MapboxDirectionsService.OptimizedOrder(List.of(0, 3, 1, 4, 2), 4000, 1800))
                .when(directions).optimize(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true));
        var result = data(mvc.perform(post("/api/v1/trips/{id}/itinerary/days/{day}/route/optimize", trip, TODAY)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        assertThat(result.path("order").size()).isEqualTo(4);
        assertThat(result.at("/order/0").asText()).isEqualTo(visits.get(2).toString());
        assertThat(result.at("/order/1").asText()).isEqualTo(visits.get(0).toString());
        assertThat(result.at("/order/2").asText()).isEqualTo(visits.get(3).toString());
        assertThat(result.at("/order/3").asText()).isEqualTo(visits.get(1).toString());
    }

    @Test void emptyDayCanPersistSettingsAndRejectsForeignReadOnlyAndInvalidInput() throws Exception {
        UUID mine = trip(owner, TODAY, TODAY, "ACTIVE", 1), foreign = trip(stranger, TODAY, TODAY, "ACTIVE", 2);
        var settings = originSettings(false);
        daySchedule(foreign, TODAY, null, settings, List.of()).andExpect(status().isNotFound());
        daySchedule(mine, TODAY.plusDays(1), null, settings, List.of()).andExpect(status().isBadRequest());
        var invalid = new HashMap<>(settings); invalid.put("origin", null);
        daySchedule(mine, TODAY, null, invalid, List.of()).andExpect(status().isBadRequest());
        daySchedule(mine, TODAY, null, settings, List.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].stops.length()").value(0));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itinerary_day_settings d JOIN itineraries i ON i.id=d.itinerary_id WHERE i.trip_id=?", Long.class, mine)).isEqualTo(1);
        jdbc.update("UPDATE trips SET status='COMPLETED' WHERE id=?", mine);
        daySchedule(mine, TODAY, null, settings, List.of()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_006"));
    }

    @Test void durationMismatchAndProviderFailureLeaveScheduleUntouched() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY, "ACTIVE", 1), place = place(province());
        link(trip, place, false); UUID itinerary = itinerary(trip); stop(itinerary, place, TODAY, 0);
        var before = data(mvc.perform(get("/api/v1/trips/{id}/itinerary", trip).header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        daySchedule(trip, TODAY, before.path("updatedAt").asText(), originSettings(true), List.of())
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.doThrow(new com.travel.common.exception.AppException(com.travel.common.exception.ErrorCode.PLACE_PROVIDER_UNAVAILABLE))
                .when(directions).route(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
        daySchedule(trip, TODAY, before.path("updatedAt").asText(), originSettings(true), List.of(Map.of("placeId", place, "minutes", 60)))
                .andExpect(status().isServiceUnavailable());
        var after = data(mvc.perform(get("/api/v1/trips/{id}/itinerary", trip).header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        assertThat(after).isEqualTo(before);
    }

    @Test void aRevisionChangeWhileRoutingIsInFlightCannotBeOverwritten() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY, "ACTIVE", 1), place = place(province());
        link(trip, place, false); UUID itinerary = itinerary(trip); stop(itinerary, place, TODAY, 0);
        var before = data(mvc.perform(get("/api/v1/trips/{id}/itinerary", trip).header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        org.mockito.Mockito.doAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            jdbc.update("UPDATE itineraries SET updated_at=updated_at+interval '1 second' WHERE id=?", itinerary);
            return com.travel.maps.dto.TripDayRoute.unavailable(com.travel.maps.dto.TripDayRoute.Status.NOT_CONFIGURED);
        }).when(directions).route(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
        daySchedule(trip, TODAY, before.path("updatedAt").asText(), originSettings(true), List.of(Map.of("placeId", place, "minutes", 60)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_008"));
        assertThat(jdbc.queryForObject("SELECT duration_minutes FROM itinerary_items WHERE itinerary_id=?", Integer.class, itinerary)).isEqualTo(45);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itinerary_day_settings WHERE itinerary_id=?", Long.class, itinerary)).isZero();
    }

    private Map<String, Object> scheduledStop(UUID place, LocalDate date, int sequence, String note) {
        return Map.of("placeId", place.toString(), "sequence", sequence, "arrivalTime", date + "T09:30:00",
                "durationMinutes", 45, "note", note);
    }

    @Test void savingOneDayPreservesOtherDaysAndHomeUsesTheUpdatedSchedule() throws Exception {
        var tomorrow = TODAY.plusDays(1);
        UUID trip = trip(owner, TODAY, tomorrow, "ACTIVE", 1);
        UUID province = province(), first = place(province), second = place(province);
        link(trip, first, false);
        link(trip, second, false);

        var firstSave = data(saveDay(trip, TODAY, null, List.of(scheduledStop(first, TODAY, 0, "Meet at gate")))
                .andExpect(status().isOk()));
        var firstId = firstSave.at("/days/0/stops/0/id").asText();
        var firstRevision = firstSave.path("updatedAt").asText();
        assertThat(home().at("/nextStop/placeId").asText()).isEqualTo(first.toString());

        var secondSave = data(saveDay(trip, tomorrow, firstRevision, List.of(scheduledStop(second, tomorrow, 0, "Bring water")))
                .andExpect(status().isOk()));
        var secondRevision = secondSave.path("updatedAt").asText();
        assertThat(secondRevision).isNotEqualTo(firstRevision);
        assertThat(secondSave.at("/days/0/stops/0/id").asText()).isEqualTo(firstId);
        assertThat(secondSave.at("/days/0/stops/0/note").asText()).isEqualTo("Meet at gate");
        saveDay(trip, TODAY, firstRevision, List.of()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_008"));

        saveDay(trip, TODAY, secondRevision, List.of()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days[0].date").value(TODAY.toString()))
                .andExpect(jsonPath("$.data.days[0].stops.length()").value(0))
                .andExpect(jsonPath("$.data.days[0].settings.departureTime").value("09:30:00"))
                .andExpect(jsonPath("$.data.days[1].date").value(tomorrow.toString()))
                .andExpect(jsonPath("$.data.days[1].stops[0].note").value("Bring water"));
        assertThat(home().path("todayStopCount").asInt()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=?", Long.class, trip)).isEqualTo(2L);
    }

    @Test void rejectsForeignUnlinkedAndOutOfRangeDayWrites() throws Exception {
        UUID foreign = trip(stranger, TODAY, TODAY, "ACTIVE", 1);
        UUID mine = trip(owner, TODAY, TODAY, "ACTIVE", 2), place = place(province());
        saveDay(foreign, TODAY, null, List.of()).andExpect(status().isNotFound());
        saveDay(mine, TODAY, null, List.of(scheduledStop(place, TODAY, 0, "")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TRIP_007"));
        saveDay(mine, TODAY.plusDays(1), null, List.of()).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itineraries WHERE trip_id=?", Long.class, mine)).isZero();
    }

    @Test void removedLegacyRowsStayOutOfReadAndAreRetainedByDaySave() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY, "ACTIVE", 1), place = place(province());
        link(trip, place, false);
        UUID itinerary = itinerary(trip);
        stop(itinerary, place, TODAY, 0);
        jdbc.update("UPDATE itinerary_items SET removed_at=now() WHERE itinerary_id=?", itinerary);
        var before = data(mvc.perform(get("/api/v1/trips/{id}/itinerary", trip)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        assertThat(before.path("days").size()).isZero();
        saveDay(trip, TODAY, before.path("updatedAt").asText(), List.of(scheduledStop(place, TODAY, 0, "")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.days[0].stops.length()").value(1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM itinerary_items WHERE itinerary_id=? AND removed_at IS NOT NULL", Long.class, itinerary)).isEqualTo(1L);
    }

    private JsonNode readItinerary(UUID id) throws Exception {
        return data(mvc.perform(get("/api/v1/trips/{id}/itinerary", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()));
    }
    private Map<String, Object> simpleSettings(String time) {
        var value = new HashMap<String, Object>();
        value.put("origin", null); value.put("departureTime", time);
        value.put("returnToStart", false); value.put("travelMode", "DRIVE");
        return value;
    }
    private void tenMinuteLegs() {
        org.mockito.Mockito.doAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            var locations = (List<?>) invocation.getArgument(0);
            assertThat(locations).allMatch(value -> {
                var point = (com.travel.maps.dto.RoutePoint)value;
                return point.latitude() != null && Math.abs(point.latitude()) <= 85.0511;
            });
            int size = locations.size() - 1;
            var legs = java.util.stream.IntStream.range(0, size)
                    .mapToObj(index -> new com.travel.maps.dto.TripDayRoute.Leg(1000, 600)).toList();
            return new com.travel.maps.dto.TripDayRoute(com.travel.maps.dto.TripDayRoute.Status.READY,
                    new com.travel.maps.dto.TripDayRoute.Route(size * 1000, size * 600, "_p~iF~ps|U_ulLnnqC", legs));
        }).when(directions).route(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
    }
    private ResultActions appendToDay(UUID trip, LocalDate day, UUID... places) throws Exception {
        return mvc.perform(post("/api/v1/trips/{id}/places/batch", trip).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("date", day, "placeIds", List.of(places)))));
    }

    @Test void estimatedTimelineRecalculatesAppendReorderDurationAndRemovalAndSurvivesReload() throws Exception {
        LocalDate date = TODAY.plusDays(1);
        UUID trip = trip(owner, date, date.plusDays(1), "PLANNING", 1), province = province();
        UUID first = place(province), second = place(province), third = place(province), other = place(province);
        link(trip, first, false); link(trip, second, false); link(trip, other, false);
        UUID itinerary = itinerary(trip);
        stop(itinerary, first, date, 0); stop(itinerary, second, date, 1); stop(itinerary, other, date.plusDays(1), 0);
        tenMinuteLegs();
        var before = readItinerary(trip);
        var saved = data(daySchedule(trip, date, before.path("updatedAt").asText(), simpleSettings("08:00"),
                List.of(Map.of("placeId", first, "minutes", 45), Map.of("placeId", second, "minutes", 60))).andExpect(status().isOk()));
        assertThat(saved.at("/days/0/stops/0/arrivalTime").asText()).isEqualTo(date + "T08:00:00");
        assertThat(saved.at("/days/0/stops/1/arrivalTime").asText()).isEqualTo(date + "T08:55:00");
        var untouchedDay = before.at("/days/1");
        appendToDay(trip, date, third).andExpect(status().isOk());
        var appended = readItinerary(trip);
        assertThat(appended.at("/days/0/stops/2/durationMinutes").asInt()).isEqualTo(45);
        assertThat(appended.at("/days/0/stops/2/arrivalTime").asText()).isEqualTo(date + "T10:05:00");
        assertThat(appended.at("/days/1")).isEqualTo(untouchedDay);
        var stops = appended.at("/days/0/stops");
        var reordered = json.createArrayNode();
        reordered.add(stops.get(1).deepCopy()); reordered.add(stops.get(0).deepCopy()); reordered.add(stops.get(2).deepCopy());
        var body = json.createObjectNode().put("expectedTripVersion", 0).put("expectedUpdatedAt", appended.path("updatedAt").asText());
        var requestStops = json.createArrayNode();
        for (int i = 0; i < reordered.size(); i++) {
            var entry = reordered.get(i);
            requestStops.addObject().put("placeId", entry.path("placeId").asText()).put("sequence", i)
                    .put("arrivalTime", stops.get(i).path("arrivalTime").asText())
                    .put("durationMinutes", entry.path("durationMinutes").asInt()).putNull("note");
        }
        body.set("stops", requestStops);
        var changed = data(mvc.perform(put("/api/v1/trips/{id}/itinerary/days/{day}", trip, date)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()));
        assertThat(changed.at("/days/0/stops/0/placeId").asText()).isEqualTo(second.toString());
        assertThat(changed.at("/days/0/stops/1/arrivalTime").asText()).isEqualTo(date + "T09:10:00");
        assertThat(changed.at("/days/0/stops/1/id")).isEqualTo(appended.at("/days/0/stops/0/id"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) requestStops.get(1)).put("durationMinutes", 60);
        // Keep actual saved arrival times; only the duration changes in this request.
        for (int i = 0; i < requestStops.size(); i++) ((com.fasterxml.jackson.databind.node.ObjectNode) requestStops.get(i))
                .put("arrivalTime", changed.at("/days/0/stops/" + i + "/arrivalTime").asText());
        body.put("expectedUpdatedAt", changed.path("updatedAt").asText());
        var resized = data(mvc.perform(put("/api/v1/trips/{id}/itinerary/days/{day}", trip, date)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()));
        assertThat(resized.at("/days/0/stops/2/arrivalTime").asText()).isEqualTo(date + "T10:20:00");
        requestStops.remove(1); ((com.fasterxml.jackson.databind.node.ObjectNode) requestStops.get(1)).put("sequence", 1);
        body.put("expectedUpdatedAt", resized.path("updatedAt").asText());
        var removed = data(mvc.perform(put("/api/v1/trips/{id}/itinerary/days/{day}", trip, date)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()));
        assertThat(removed.at("/days/0/stops/1/arrivalTime").asText()).isEqualTo(date + "T09:10:00");
        assertThat(readItinerary(trip)).isEqualTo(removed);
        assertThat(removed.at("/days/1")).isEqualTo(untouchedDay);
    }

    @Test void liveTimelinePreservesVisitedTimesAndRejectsChangingVisitedDurationOrRemovingVisitedStop() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY, "ACTIVE", 1), province = province();
        UUID first = place(province), second = place(province), third = place(province);
        link(trip, first, true); link(trip, second, false);
        UUID itinerary = itinerary(trip); stop(itinerary, first, TODAY, 0); stop(itinerary, second, TODAY, 1);
        tenMinuteLegs();
        var before = readItinerary(trip);
        var saved = data(daySchedule(trip, TODAY, before.path("updatedAt").asText(), simpleSettings("08:00"),
                List.of(Map.of("placeId", first, "minutes", 45), Map.of("placeId", second, "minutes", 60))).andExpect(status().isOk()));
        assertThat(saved.at("/days/0/stops/0/arrivalTime")).isEqualTo(before.at("/days/0/stops/0/arrivalTime"));
        assertThat(saved.at("/days/0/stops/1/arrivalTime").asText()).isEqualTo(TODAY + "T09:55:00");
        daySchedule(trip, TODAY, saved.path("updatedAt").asText(), simpleSettings("08:00"),
                List.of(Map.of("placeId", first, "minutes", 60), Map.of("placeId", second, "minutes", 60)))
                .andExpect(status().isConflict());
        saveDay(trip, TODAY, saved.path("updatedAt").asText(), List.of(scheduledStop(second, TODAY, 0, "")))
                .andExpect(status().isConflict());
        appendToDay(trip, TODAY, third).andExpect(status().isOk());
        assertThat(readItinerary(trip).at("/days/0/stops/2/arrivalTime").asText()).isEqualTo(TODAY + "T11:05:00");
    }

    @Test void noRouteLeavesUnknownArrivalsUnsetAndNoDepartureLeavesNewStopsUnscheduledInTimeOnly() throws Exception {
        LocalDate date = TODAY.plusDays(1);
        UUID trip = trip(owner, date, date, "PLANNING", 1), first = place(province()), second = place(province());
        appendToDay(trip, date, first, second).andExpect(status().isOk());
        var initial = readItinerary(trip);
        assertThat(initial.at("/days/0/stops/0/arrivalTime").isNull()).isTrue();
        assertThat(initial.at("/days/0/stops/0/durationMinutes").asInt()).isEqualTo(45);
        var saved = data(daySchedule(trip, date, initial.path("updatedAt").asText(), simpleSettings("08:00"),
                List.of(Map.of("placeId", first, "minutes", 45), Map.of("placeId", second, "minutes", 45))).andExpect(status().isOk()));
        assertThat(saved.at("/days/0/stops/0/arrivalTime").asText()).isEqualTo(date + "T08:00:00");
        assertThat(saved.at("/days/0/stops/1/arrivalTime").isNull()).isTrue();
    }

    @Test void concurrentScheduleChangeDuringAppendDoesNotPartiallyAddMemberships() throws Exception {
        LocalDate date = TODAY.plusDays(1);
        UUID trip = trip(owner, date, date, "PLANNING", 1), first = place(province()), second = place(province());
        appendToDay(trip, date, first).andExpect(status().isOk());
        var before = readItinerary(trip);
        daySchedule(trip, date, before.path("updatedAt").asText(), simpleSettings("08:00"),
                List.of(Map.of("placeId", first, "minutes", 45))).andExpect(status().isOk());
        org.mockito.Mockito.doAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            jdbc.update("UPDATE itineraries SET updated_at=updated_at+interval '1 second' WHERE trip_id=?", trip);
            return com.travel.maps.dto.TripDayRoute.unavailable(com.travel.maps.dto.TripDayRoute.Status.NOT_CONFIGURED);
        }).when(directions).route(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
        appendToDay(trip, date, second).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_008"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM trip_places WHERE trip_id=? AND place_id=?", Long.class, trip, second)).isZero();
        assertThat(readItinerary(trip).at("/days/0/stops").size()).isEqualTo(1);
    }

    @Test void calculatedScheduleCannotRunPastMidnight() throws Exception {
        LocalDate date = TODAY.plusDays(1);
        UUID trip = trip(owner, date, date, "PLANNING", 1), first = place(province()), second = place(province());
        appendToDay(trip, date, first, second).andExpect(status().isOk());
        tenMinuteLegs(); var before = readItinerary(trip);
        daySchedule(trip, date, before.path("updatedAt").asText(), simpleSettings("23:30"),
                List.of(Map.of("placeId", first, "minutes", 45), Map.of("placeId", second, "minutes", 45)))
                .andExpect(status().isBadRequest());
        assertThat(readItinerary(trip)).isEqualTo(before);
    }


    @Test void explicitArrivalEditRecalculatesFollowingStopsButNoteOnlyEditDoesNotCallProvider() throws Exception {
        LocalDate date = TODAY.plusDays(1);
        UUID trip = trip(owner, date, date, "PLANNING", 1), first = place(province()), second = place(province());
        appendToDay(trip, date, first, second).andExpect(status().isOk());
        tenMinuteLegs(); var before = readItinerary(trip);
        var saved = data(daySchedule(trip, date, before.path("updatedAt").asText(), simpleSettings("08:00"),
                List.of(Map.of("placeId", first, "minutes", 45), Map.of("placeId", second, "minutes", 45))).andExpect(status().isOk()));
        var stops = List.of(scheduledStop(first, date, 0, "Meet here"), Map.<String, Object>of(
                "placeId", second, "sequence", 1, "arrivalTime", saved.at("/days/0/stops/1/arrivalTime").asText(),
                "durationMinutes", 45, "note", ""));
        var changed = data(saveDay(trip, date, saved.path("updatedAt").asText(), stops).andExpect(status().isOk()));
        assertThat(changed.at("/days/0/stops/0/arrivalTime").asText()).isEqualTo(date + "T09:30:00");
        assertThat(changed.at("/days/0/stops/1/arrivalTime").asText()).isEqualTo(date + "T10:25:00");
        org.mockito.Mockito.doThrow(new com.travel.common.exception.AppException(com.travel.common.exception.ErrorCode.PLACE_PROVIDER_UNAVAILABLE))
                .when(directions).route(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
        var notes = List.of(scheduledStop(first, date, 0, "Changed note only"), Map.<String, Object>of(
                "placeId", second, "sequence", 1, "arrivalTime", changed.at("/days/0/stops/1/arrivalTime").asText(),
                "durationMinutes", 45, "note", ""));
        var noted = data(saveDay(trip, date, changed.path("updatedAt").asText(), notes).andExpect(status().isOk()));
        assertThat(noted.at("/days/0/stops/0/note").asText()).isEqualTo("Changed note only");
        assertThat(noted.at("/days/0/stops/1/arrivalTime")).isEqualTo(changed.at("/days/0/stops/1/arrivalTime"));
    }


    @Test void firstTimedStopIsEnoughToScheduleFollowingStopsWithoutDaySettings() throws Exception {
        LocalDate date = TODAY.plusDays(1);
        UUID trip = trip(owner, date, date, "PLANNING", 1), province = province();
        UUID first = place(province), second = place(province), third = place(province);
        link(trip, first, false); link(trip, second, false);
        UUID itinerary = itinerary(trip); stop(itinerary, first, date, 0); stop(itinerary, second, date, 1);
        jdbc.update("UPDATE itinerary_items SET arrival_at=NULL,start_time=NULL,end_time=NULL WHERE itinerary_id=? AND place_id=?", itinerary, second);
        tenMinuteLegs(); var before = readItinerary(trip);
        var body = json.createObjectNode().put("expectedTripVersion", 0).put("expectedUpdatedAt", before.path("updatedAt").asText());
        var stops = body.putArray("stops");
        stops.addObject().put("placeId", first.toString()).put("sequence", 0).put("arrivalTime", date + "T08:00:00").put("durationMinutes", 45).putNull("note");
        stops.addObject().put("placeId", second.toString()).put("sequence", 1).putNull("arrivalTime").put("durationMinutes", 45).putNull("note");
        var saved = data(mvc.perform(put("/api/v1/trips/{id}/itinerary/days/{day}", trip, date)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().isOk()));
        assertThat(saved.at("/days/0/stops/0/arrivalTime").asText()).isEqualTo(date + "T08:00:00");
        assertThat(saved.at("/days/0/stops/1/arrivalTime").asText()).isEqualTo(date + "T08:55:00");
        assertThat(saved.at("/days/0/settings/departureTime").asText()).isEqualTo("08:00:00");
        appendToDay(trip, date, third).andExpect(status().isOk());
        assertThat(readItinerary(trip).at("/days/0/stops/2/arrivalTime").asText()).isEqualTo(date + "T09:50:00");
    }

    @Test void addingToLegacyDayInfersDepartureFromFirstExistingArrival() throws Exception {
        LocalDate date = TODAY.plusDays(1);
        UUID trip = trip(owner, date, date, "PLANNING", 1), first = place(province()), second = place(province());
        link(trip, first, false); UUID itinerary = itinerary(trip); stop(itinerary, first, date, 0);
        tenMinuteLegs(); appendToDay(trip, date, second).andExpect(status().isOk());
        var saved = readItinerary(trip);
        assertThat(saved.at("/days/0/stops/0/arrivalTime").asText()).isEqualTo(date + "T09:00:00");
        assertThat(saved.at("/days/0/stops/1/arrivalTime").asText()).isEqualTo(date + "T09:55:00");
        assertThat(saved.at("/days/0/settings/departureTime").asText()).isEqualTo("09:00:00");
    }

    private void remainingMatrix(boolean returns) {
        org.mockito.Mockito.doAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat((List<?>) invocation.getArgument(0)).hasSize(returns ? 4 : 3);
            assertThat(((com.travel.maps.dto.RoutePoint)((List<?>)invocation.getArgument(0)).getFirst()).latitude()).isEqualTo(16.02);
            return returns ? new com.travel.maps.service.MapboxDirectionsService.RoadMatrix(
                    new double[][]{{0,9000,1000,9000},{9000,0,9000,1000},{9000,1000,0,9000},{9000,9000,9000,0}},
                    new double[][]{{0,3600,600,3600},{3600,0,3600,600},{3600,600,0,3600},{3600,3600,3600,0}})
                    : new com.travel.maps.service.MapboxDirectionsService.RoadMatrix(
                    new double[][]{{0,9000,1000},{9000,0,9000},{9000,1000,0}},
                    new double[][]{{0,3600,600},{3600,0,3600},{3600,600,0}});
        }).when(directions).matrix(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.any());
    }
    private ResultActions applyVisitedOrder(UUID trip, JsonNode base, List<UUID> order, List<UUID> visited) throws Exception {
        var byPlace = new HashMap<String, JsonNode>();
        for (var entry : base.at("/days/0/stops")) byPlace.put(entry.path("placeId").asText(), entry);
        var body = json.createObjectNode().put("expectedTripVersion", 0).put("expectedUpdatedAt", base.path("updatedAt").asText());
        body.set("expectedVisitedPlaceIds", json.valueToTree(visited));
        var stops = body.putArray("stops");
        for (int index = 0; index < order.size(); index++) {
            var old = byPlace.get(order.get(index).toString());
            var next = stops.addObject().put("placeId", order.get(index).toString()).put("sequence", index)
                    .put("durationMinutes", old.path("durationMinutes").asInt());
            next.set("arrivalTime", old.path("arrivalTime")); next.set("note", old.path("note"));
        }
        return mvc.perform(put("/api/v1/trips/{id}/itinerary/days/{day}", trip, TODAY)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body.toString()));
    }

    @Test void optimizationGroupsVisitedACAndOptimizesOnlyBDThenPersistsACDBAndTheirNewTimes() throws Exception {
        UUID trip = trip(owner, TODAY, TODAY.plusDays(1), "ACTIVE", 1), province = province(), itinerary = itinerary(trip);
        UUID a = place(province), b = place(province), c = place(province), d = place(province), tomorrow = place(province);
        var visits = List.of(a,b,c,d);
        for (int index=0; index<visits.size(); index++) { link(trip,visits.get(index),index==0 || index==2); stop(itinerary,visits.get(index),TODAY,index); }
        link(trip,tomorrow,false); stop(itinerary,tomorrow,TODAY.plusDays(1),0);
        jdbc.update("UPDATE places SET latitude=89 WHERE id=?", a);
        jdbc.update("UPDATE places SET latitude=16.02 WHERE id=?", c);
        var before = readItinerary(trip);
        remainingMatrix(false); tenMinuteLegs();
        var proposal = data(mvc.perform(post("/api/v1/trips/{id}/itinerary/days/{day}/route/optimize", trip, TODAY)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk()));
        assertThat(proposal.path("order")).extracting(node -> node.asText()).containsExactly(a.toString(),c.toString(),d.toString(),b.toString());
        assertThat(proposal.path("originalOrder")).extracting(node -> node.asText()).containsExactly(a.toString(),b.toString(),c.toString(),d.toString());
        assertThat(proposal.path("visitedOrder")).extracting(node -> node.asText()).containsExactly(a.toString(),c.toString());
        assertThat(proposal.path("originalDistanceMeters").asDouble()).isEqualTo(18000);
        assertThat(proposal.path("distanceMeters").asDouble()).isEqualTo(2000);
        assertThat(proposal.path("originalDurationSeconds").asDouble()).isEqualTo(7200);
        assertThat(proposal.path("durationSeconds").asDouble()).isEqualTo(1200);
        assertThat(readItinerary(trip)).isEqualTo(before); // Preview has no side effects.
        var applied = data(applyVisitedOrder(trip,before,List.of(a,c,d,b),List.of(a,c)).andExpect(status().isOk()));
        assertThat(applied.at("/days/0/stops/0/id")).isEqualTo(before.at("/days/0/stops/0/id"));
        assertThat(applied.at("/days/0/stops/1/id")).isEqualTo(before.at("/days/0/stops/2/id"));
        assertThat(applied.at("/days/0/stops/0/arrivalTime")).isEqualTo(before.at("/days/0/stops/0/arrivalTime"));
        assertThat(applied.at("/days/0/stops/1/arrivalTime")).isEqualTo(before.at("/days/0/stops/2/arrivalTime"));
        assertThat(applied.at("/days/0/stops/1/durationMinutes").asInt()).isEqualTo(45);
        assertThat(applied.at("/days/0/stops/2/placeId").asText()).isEqualTo(d.toString());
        assertThat(applied.at("/days/0/stops/2/arrivalTime").asText()).isEqualTo(TODAY+"T11:55:00");
        assertThat(applied.at("/days/0/stops/3/arrivalTime").asText()).isEqualTo(TODAY+"T12:50:00");
        assertThat(applied.at("/days/1")).isEqualTo(before.at("/days/1"));
        assertThat(readItinerary(trip)).isEqualTo(applied);
        assertThat(home().at("/nextStop/placeId").asText()).isEqualTo(d.toString());
        appendToDay(trip, TODAY, tomorrow).andExpect(status().isOk());
        var appended = readItinerary(trip);
        assertThat(appended.at("/days/0/stops/4/arrivalTime").asText()).isEqualTo(TODAY+"T13:45:00");
        assertThat(appended.at("/days/0/stops/1/arrivalTime")).isEqualTo(before.at("/days/0/stops/2/arrivalTime"));
        assertThat(appended.at("/days/1")).isEqualTo(before.at("/days/1"));
    }

    @Test void remainingOptimizationIncludesReturnToSavedOriginRatherThanReturningToVisitedAnchor() throws Exception {
        UUID trip=trip(owner,TODAY,TODAY,"ACTIVE",1), province=province(), itinerary=itinerary(trip);
        UUID a=place(province), b=place(province), c=place(province), d=place(province);
        var visits=List.of(a,b,c,d);
        for(int i=0;i<4;i++){link(trip,visits.get(i),i==0||i==2);stop(itinerary,visits.get(i),TODAY,i);}
        jdbc.update("INSERT INTO itinerary_day_settings(id,itinerary_id,day_date,origin_name,origin_latitude,origin_longitude,departure_time,return_to_start,travel_mode) VALUES (?,?,?,'Origin',16.01,108.01,'08:00',true,'DRIVE')",UUID.randomUUID(),itinerary,TODAY);
        jdbc.update("UPDATE places SET latitude=16.02 WHERE id=?",c);
        remainingMatrix(true);
        var proposal=data(mvc.perform(post("/api/v1/trips/{id}/itinerary/days/{day}/route/optimize",trip,TODAY)
                .header("Authorization","Bearer "+token)).andExpect(status().isOk()));
        assertThat(proposal.path("order")).extracting(node->node.asText()).containsExactly(a.toString(),c.toString(),d.toString(),b.toString());
        assertThat(proposal.path("distanceMeters").asDouble()).isEqualTo(3000);
        assertThat(proposal.path("durationSeconds").asDouble()).isEqualTo(1800);
    }

    @Test void staleVisitedSnapshotOrTamperedVisitedPrefixCannotOverwriteCurrentSchedule() throws Exception {
        UUID trip=trip(owner,TODAY,TODAY,"ACTIVE",1), province=province(), itinerary=itinerary(trip);
        UUID a=place(province), b=place(province), c=place(province), d=place(province);
        var visits=List.of(a,b,c,d);
        for(int i=0;i<4;i++){link(trip,visits.get(i),i==0||i==2);stop(itinerary,visits.get(i),TODAY,i);}
        tenMinuteLegs(); var before=readItinerary(trip);
        applyVisitedOrder(trip,before,List.of(c,a,b,d),List.of(a,c)).andExpect(status().isBadRequest());
        applyVisitedOrder(trip,before,List.of(a,c,b),List.of(a,c)).andExpect(status().isBadRequest());
        jdbc.update("UPDATE trip_places SET visited=true WHERE trip_id=? AND place_id=?",trip,b);
        applyVisitedOrder(trip,before,List.of(a,c,d,b),List.of(a,c)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_008"));
        assertThat(readItinerary(trip)).isEqualTo(before);
        jdbc.update("UPDATE trip_places SET visited=false WHERE trip_id=? AND place_id=?",trip,b);
        jdbc.update("UPDATE trip_places SET visited=false WHERE trip_id=? AND place_id=?",trip,c);
        applyVisitedOrder(trip,before,List.of(a,c,d,b),List.of(a,c)).andExpect(status().isConflict());
        assertThat(readItinerary(trip)).isEqualTo(before);
    }

    @Test void groupingStillRejectsEditsToVisitedArrivalTimes() throws Exception {
        UUID trip=trip(owner,TODAY,TODAY,"ACTIVE",1), province=province(), itinerary=itinerary(trip);
        UUID a=place(province), b=place(province), c=place(province);
        link(trip,a,true);link(trip,b,false);link(trip,c,true);
        stop(itinerary,a,TODAY,0);stop(itinerary,b,TODAY,1);stop(itinerary,c,TODAY,2);
        tenMinuteLegs(); var before=readItinerary(trip);
        var tampered=before.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode)tampered.at("/days/0/stops/2")).put("arrivalTime",TODAY+"T10:30:00");
        applyVisitedOrder(trip,tampered,List.of(a,c,b),List.of(a,c)).andExpect(status().isConflict());
        assertThat(readItinerary(trip)).isEqualTo(before);
    }

    @Test void completedTripHomeAndDayAlbumUseOwnerDataWithLocalDateBoundariesAndStayReadOnly() throws Exception {
        UUID id=trip(owner,TODAY.minusDays(2),TODAY.minusDays(1),"COMPLETED",1), province=province(), first=place(province);
        link(id,first,true); UUID itinerary=itinerary(id); stop(itinerary,first,TODAY.minusDays(1),0);
        Instant start=TODAY.minusDays(1).atStartOfDay(com.travel.trip.service.TripCalendar.ZONE).toInstant();
        UUID before=memory(owner,id,first,start.minusSeconds(1));photo(before,0);
        UUID atStart=memory(owner,id,first,start);photo(atStart,0);photo(atStart,1);
        UUID atEnd=memory(owner,id,first,start.plusSeconds(86400).minusSeconds(1));
        UUID nextDay=memory(owner,id,first,start.plusSeconds(86400));photo(nextDay,0);
        UUID deleted=memory(owner,id,first,start.plusSeconds(100));photo(deleted,0);
        jdbc.update("UPDATE memories SET deleted_at=now() WHERE id=?",deleted);
        UUID foreign=memory(stranger,id,first,start.plusSeconds(200));photo(foreign,0);
        var dashboard=home();
        assertThat(dashboard.path("state").asText()).isEqualTo("RETURNING");
        assertThat(dashboard.at("/selectedTrip/trip/id").asText()).isEqualTo(id.toString());
        assertThat(dashboard.at("/selectedTrip/trip/mode").asText()).isEqualTo("RECAP");
        assertThat(dashboard.at("/selectedTrip/trip/stopCount").asInt()).isEqualTo(1);
        assertThat(dashboard.at("/selectedTrip/trip/memoryCount").asInt()).isEqualTo(4);
        assertThat(dashboard.path("recentPhotos")).extracting(node->node.path("memoryId").asText()).doesNotContain(foreign.toString(),deleted.toString());
        var album=data(mvc.perform(get("/api/v1/journal/memories").param("tripId",id.toString())
                .param("date",TODAY.minusDays(1).toString()).param("size","1")
                .header("Authorization","Bearer "+token)).andExpect(status().isOk()));
        assertThat(album.path("totalElements").asInt()).isEqualTo(2);
        assertThat(album.at("/content/0/id").asText()).isEqualTo(atEnd.toString());
        assertThat(album.at("/content/0/media").size()).isZero();
        var page2=data(mvc.perform(get("/api/v1/journal/memories").param("tripId",id.toString())
                .param("date",TODAY.minusDays(1).toString()).param("size","1").param("page","1")
                .header("Authorization","Bearer "+token)).andExpect(status().isOk()));
        assertThat(page2.at("/content/0/id").asText()).isEqualTo(atStart.toString());
        assertThat(page2.at("/content/0/media").size()).isEqualTo(2);
        var base=readItinerary(id);
        saveDay(id,TODAY.minusDays(1),base.path("updatedAt").asText(),List.of()).andExpect(status().isConflict());
        assertThat(readItinerary(id)).isEqualTo(base);
        UUID other=trip(stranger,TODAY.minusDays(2),TODAY.minusDays(1),"COMPLETED",2);
        mvc.perform(get("/api/v1/journal/memories").param("tripId",other.toString()).param("date",TODAY.minusDays(1).toString())
                .header("Authorization","Bearer "+token)).andExpect(status().isNotFound());
    }

}
