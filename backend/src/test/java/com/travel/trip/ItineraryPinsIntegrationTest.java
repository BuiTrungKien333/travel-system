package com.travel.trip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.mail.AuthEmailSender;
import com.travel.place.enums.PlaceType;
import com.travel.planning.dto.request.MultiDayOptimizeAndAuditRequest;
import com.travel.planning.dto.response.DayScheduleResponse;
import com.travel.planning.dto.response.MultiDayOptimizeAndAuditResponse;
import com.travel.planning.rule.model.ItineraryEvaluationResult;
import com.travel.planning.rule.model.ScheduleItemContext;
import com.travel.planning.service.TripOptimizationService;
import com.travel.user.entity.User;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
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
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ItineraryPinsIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @MockitoBean AuthEmailSender sender;
    @MockitoBean TripOptimizationService optimizer;
    MockMvc mvc;
    String token;
    UUID tripId;
    LocalDate date;
    List<UUID> places;

    @BeforeEach void setup() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        var user = users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.com").fullName("Pins")
                .passwordHash(encoder.encode("fixture-password")).emailVerifiedAt(Instant.now()).build());
        token = data(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email",user.getEmail(),"password","fixture-password"))))
                .andExpect(status().isOk())).path("accessToken").asText();
        date = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        tripId = UUID.fromString(data(mvc.perform(post("/api/v1/trips").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("clientRequestId", UUID.randomUUID(),
                        "destination",Map.of("name","Da Lat"),"startDate",date.toString(),"endDate",date.plusDays(1).toString()))))
                .andExpect(status().isCreated())).path("trip").path("id").asText());
        places = List.of(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());
        for (UUID id : places) {
            jdbc.update("INSERT INTO places(id,name,latitude,longitude) VALUES (?,'Fixture',11.94,108.44)",id);
            jdbc.update("INSERT INTO trip_places(trip_id,place_id) VALUES (?,?)",tripId,id);
        }
    }
    JsonNode data(ResultActions action) throws Exception { return json.readTree(action.andReturn().getResponse().getContentAsString()).path("data"); }
    Map<String,Object> schedule(boolean pinned) {
        List<Map<String,Object>> stops = new ArrayList<>();
        for (int i=0; i<places.size(); i++) stops.add(new LinkedHashMap<>(Map.of("placeId",places.get(i),"sequence",i,
                "arrivalTime",date.atTime(10+i,0).toString(),"durationMinutes",30,"note","Note "+i,"pinned",pinned && i==1)));
        return new LinkedHashMap<>(Map.of("days",List.of(Map.of("date",date.toString(),"stops",stops))));
    }
    ResultActions save(Map<String,?> request) throws Exception {
        return mvc.perform(put("/api/v1/trips/"+tripId+"/itinerary").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)));
    }
    ResultActions readItinerary() throws Exception {
        return mvc.perform(get("/api/v1/trips/"+tripId+"/itinerary").header("Authorization","Bearer "+token));
    }
    MultiDayOptimizeAndAuditResponse proposal(List<UUID> order, int pinHour) {
        List<ScheduleItemContext> items = new ArrayList<>();
        for (int i=0; i<order.size(); i++) items.add(new ScheduleItemContext(order.get(i),"Fixture",11.94,108.44,PlaceType.UNSPECIFIED,
                null,null,date.atTime(order.get(i).equals(places.get(1)) ? pinHour : 10+i,0),30,0,0));
        return new MultiDayOptimizeAndAuditResponse("ALL_CLEAR",0,0,
                List.of(new DayScheduleResponse(date,"ALL_CLEAR",0,0,items,ItineraryEvaluationResult.of(List.of()))),List.of(),null);
    }
    ResultActions apply(String stamp, MultiDayOptimizeAndAuditResponse proposal) throws Exception {
        return mvc.perform(post("/api/v1/trips/"+tripId+"/itinerary/apply-optimization").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("expectedUpdatedAt",stamp,"proposal",proposal))));
    }

    @Test void pinsRoundTripAndOmittedFlagsArePreservedWhileExplicitFalseUnpins() throws Exception {
        var first = data(save(schedule(true)).andExpect(status().isOk()));
        assertThat(first.path("days").get(0).path("stops").get(1).path("pinned").asBoolean()).isTrue();
        readItinerary().andExpect(status().isOk()).andExpect(jsonPath("$.data.days[0].stops[0].pinned").value(false))
                .andExpect(jsonPath("$.data.days[0].stops[1].pinned").value(true));
        var legacy = json.valueToTree(schedule(false));
        for (var stop : legacy.path("days").get(0).path("stops")) ((com.fasterxml.jackson.databind.node.ObjectNode)stop).remove("pinned");
        Map<String,Object> body = json.convertValue(legacy,new com.fasterxml.jackson.core.type.TypeReference<>(){});
        body.put("expectedUpdatedAt",first.path("updatedAt").asText());
        var preserved = data(save(body).andExpect(status().isOk()));
        assertThat(preserved.path("days").get(0).path("stops").get(1).path("pinned").asBoolean()).isTrue();
        assertThat(preserved.path("updatedAt").asText()).isNotEqualTo(first.path("updatedAt").asText());
        var unpin = schedule(false); unpin.put("expectedUpdatedAt",preserved.path("updatedAt").asText());
        save(unpin).andExpect(status().isOk()).andExpect(jsonPath("$.data.days[0].stops[1].pinned").value(false));
        save(body).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_008"));
    }
    @Test void optimizerReceivesPersistedPinAndApplyingCannotMoveItsSlotOrTime() throws Exception {
        var saved = data(save(schedule(true)).andExpect(status().isOk()));
        var valid = proposal(places,11);
        when(optimizer.optimizeAndAuditMultiDay(any())).thenReturn(valid);
        mvc.perform(post("/api/v1/trips/"+tripId+"/itinerary/optimize").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sourceUpdatedAt").value(saved.path("updatedAt").asText()));
        var capture = ArgumentCaptor.forClass(MultiDayOptimizeAndAuditRequest.class);
        verify(optimizer).optimizeAndAuditMultiDay(capture.capture());
        var pin = capture.getValue().days().getFirst().locations().get(1);
        assertThat(pin.pinnedSequence()).isEqualTo(1);
        assertThat(pin.pinnedArrivalTime()).isEqualTo(date.atTime(11,0));
        String stamp = saved.path("updatedAt").asText();
        apply(stamp,proposal(List.of(places.get(1),places.get(0),places.get(2)),11))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_014"));
        apply(stamp,proposal(places,12)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_014"));
        apply(stamp,valid).andExpect(status().isOk()).andExpect(jsonPath("$.data.days[0].stops[1].pinned").value(true))
                .andExpect(jsonPath("$.data.days[0].stops[1].note").value("Note 1"));
    }
    @Test void changingThePinInvalidatesAnEarlierProposal() throws Exception {
        var saved = data(save(schedule(true)).andExpect(status().isOk()));
        var unpin = schedule(false); unpin.put("expectedUpdatedAt",saved.path("updatedAt").asText());
        save(unpin).andExpect(status().isOk());
        apply(saved.path("updatedAt").asText(),proposal(places,11)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_008"));
    }
    @Test void invalidPinAndReadOnlyOrUnownedTripsDoNotWrite() throws Exception {
        var invalid = json.valueToTree(schedule(true));
        ((com.fasterxml.jackson.databind.node.ObjectNode)invalid.path("days").get(0).path("stops").get(1)).putNull("arrivalTime");
        save(json.convertValue(invalid,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){})).andExpect(status().isBadRequest());
        readItinerary().andExpect(status().isOk()).andExpect(jsonPath("$.data.days").isEmpty());
        mvc.perform(put("/api/v1/trips/"+tripId+"/itinerary").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(schedule(true)))).andExpect(status().isUnauthorized());
        jdbc.update("UPDATE trips SET status='CANCELLED' WHERE id=?",tripId);
        save(schedule(true)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIP_013"));
        var other = users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.com").fullName("Other").build());
        jdbc.update("UPDATE trips SET user_id=? WHERE id=?",other.getId(),tripId);
        save(schedule(true)).andExpect(status().isNotFound());
    }
    @Test void changingTripDatesInvalidatesAnOpenScheduleEditor() throws Exception {
        var saved = data(save(schedule(true)).andExpect(status().isOk()));
        mvc.perform(patch("/api/v1/trips/"+tripId).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("version",0,"startDate",date.toString(),"endDate",date.plusDays(2).toString()))))
                .andExpect(status().isOk());
        var stale = schedule(false); stale.put("expectedUpdatedAt",saved.path("updatedAt").asText());
        save(stale).andExpect(status().isConflict());
        readItinerary().andExpect(status().isOk()).andExpect(jsonPath("$.data.days[0].stops[1].pinned").value(true));
    }
}
