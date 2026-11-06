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
}
