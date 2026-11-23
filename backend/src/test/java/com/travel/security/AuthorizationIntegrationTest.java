package com.travel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.TestcontainersConfiguration;
import com.travel.auth.service.AuthTokenIssuer;
import com.travel.security.jwt.JwtUtils;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.entity.User;
import com.travel.user.enums.Role;
import com.travel.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthorizationIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired JwtUtils jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthTokenIssuer issuer;
    @Autowired PlatformTransactionManager transactions;
    MockMvc mvc;
    User member, admin, guest;
    String memberToken, adminToken, guestToken;
    ObjectMapper json = new ObjectMapper();

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        member = account(Role.USER); admin = account(Role.ADMIN); guest = account(Role.GUEST);
        memberToken=token(member); adminToken=token(admin); guestToken=token(guest);
    }
    User account(Role role) {
        return users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.com").fullName("RBAC test")
                .role(role).passwordHash("unused").emailVerifiedAt(Instant.now()).build());
    }
    String token(User user) {
        // Empty claims permissions intentionally prove that current database grants are authoritative.
        return jwt.generateAccessToken(new CustomUserDetails(user));
    }

    @ParameterizedTest
    @ValueSource(strings={"/api/v1/trips","/api/v1/home/dashboard","/api/v1/journal/memories","/api/v1/media/limits",
            "/api/v1/places/suggestions","/api/v1/trips/00000000-0000-0000-0000-000000000001/planning/drafts/latest"})
    void guestTokenCannotEnterMemberApis(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_001"));
        mvc.perform(get(path).header("Authorization","Bearer "+guestToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_002"));
    }
    @Test void dashboardAndKnowledgeAreAdminOnlyAndReturnRealAggregates() throws Exception {
        mvc.perform(get("/api/v1/admin/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/overview").header("Authorization","Bearer "+memberToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/overview").param("days","7").header("Authorization","Bearer "+adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.days").value(7)).andExpect(jsonPath("$.data.trend.length()").value(7));
        UUID province=province();UUID id=place(province,"INTERNAL",true,"EXISTENCE_ONLY");
        mvc.perform(get("/api/v1/admin/knowledge").header("Authorization","Bearer "+adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.indexAvailable").value(false));
        mvc.perform(get("/api/v1/admin/knowledge").header("Authorization","Bearer "+guestToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/overview").param("days","999").header("Authorization","Bearer "+adminToken)).andExpect(status().isBadRequest());
    }

    @Test void membersAndAdminsCanUseOwnWorkspaceButOnlyAdminCanListAccounts() throws Exception {
        for(String token:List.of(memberToken,adminToken))
            mvc.perform(get("/api/v1/trips").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+memberToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/unknown-future-endpoint").header("Authorization","Bearer "+adminToken)).andExpect(status().isForbidden());
    }
    @Test void telemetryIsAdminOnlyAndExplicitlyDisabledWithoutMongo() throws Exception {
        mvc.perform(get("/api/v1/admin/ai/executions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/ai/executions").header("Authorization","Bearer "+memberToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/ai/executions").header("Authorization","Bearer "+adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DISABLED")).andExpect(jsonPath("$.data.data").doesNotExist());
    }
    @Test void adminSessionExposesCurrentCapabilitiesWithoutTokensOrPrivateCredentials() throws Exception {
        mvc.perform(get("/api/v1/admin/session")).andExpect(status().isUnauthorized());
        for (String token : List.of(memberToken, guestToken))
            mvc.perform(get("/api/v1/admin/session").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/session").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(admin.getId().toString()))
                .andExpect(jsonPath("$.data.permissions", org.hamcrest.Matchers.hasItem("admin:users")))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist()).andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }
    @Test void publicReadsAndGuestSelfInspectionWorkButGuestCannotUpdateProfile() throws Exception {
        mvc.perform(get("/api/v1/destinations")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.data.database").doesNotExist());
        mvc.perform(get("/api/v1/users/me").header("Authorization","Bearer "+guestToken)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/users/me").header("Authorization","Bearer "+guestToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Changed\"}" )).andExpect(status().isForbidden());
    }
    @ParameterizedTest
    @ValueSource(strings={"/api/v1/public/planning/generate-draft","/api/v1/public/planning/retrieval/index",
            "/api/v1/public/planning/retrieval/intent","/api/v1/public/planning/itinerary/optimize-and-audit",
            "/api/v1/public/planning/tsp/optimize","/api/v1/public/planning/rules/evaluate"})
    void publicNamedToolingRequiresAuthorization(String path) throws Exception {
        mvc.perform(post(path)).andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("Authorization","Bearer "+guestToken)).andExpect(status().isForbidden());
    }
    @Test void adminCannotReadAnotherPersonsTripOrItinerary() throws Exception {
        UUID trip=UUID.randomUUID();
        jdbc.update("INSERT INTO trips(id,user_id,name,destination,status) VALUES (?,?,?,'Da Nang','PLANNING')",trip,member.getId(),"Private");
        for(String suffix:List.of("","/itinerary","/weather"))
            mvc.perform(get("/api/v1/trips/"+trip+suffix).header("Authorization","Bearer "+adminToken)).andExpect(status().isNotFound());
    }
    @Test void lockingInvalidatesAccessAndRefreshAndUnlockingDoesNotReviveOldTokens() throws Exception {
        var auth = new TransactionTemplate(transactions).execute(s -> issuer.issue(users.findById(member.getId()).orElseThrow()));
        change(member,Role.USER,true,0,adminToken).andExpect(status().isOk());
        mvc.perform(get("/api/v1/trips").header("Authorization","Bearer "+auth.getAccessToken())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken",auth.getRefreshToken())))).andExpect(status().isForbidden());
        change(member,Role.USER,false,1,adminToken).andExpect(status().isOk());
        mvc.perform(get("/api/v1/trips").header("Authorization","Bearer "+auth.getAccessToken())).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_account_events WHERE target_id=?",Integer.class,member.getId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_refresh_tokens WHERE user_id=?",Integer.class,member.getId())).isZero();
        mvc.perform(get("/api/v1/admin/users/events").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].actorEmail").value(admin.getEmail()))
                .andExpect(jsonPath("$.data.content[0].targetEmail").value(member.getEmail()));
    }
    @Test void roleChangeRevokesOldAdminTokenAndStaleUpdatesConflict() throws Exception {
        var other=account(Role.ADMIN);String old=token(other);
        change(other,Role.USER,false,0,adminToken).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+old)).andExpect(status().isUnauthorized());
        change(other,Role.GUEST,false,0,adminToken).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+token(users.findById(other.getId()).orElseThrow())))
                .andExpect(status().isForbidden());
    }
    @Test void selfLockAndSelfDemotionAreRejectedWithoutAuditOrVersionChange() throws Exception {
        change(admin,Role.USER,false,0,adminToken).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ADMIN_002"));
        change(admin,Role.ADMIN,true,0,adminToken).andExpect(status().isConflict());
        assertThat(users.findById(admin.getId()).orElseThrow().getSessionVersion()).isZero();
    }
    @Test void currentPermissionsOverridePermissionsInAnExistingToken() throws Exception {
        jdbc.update("DELETE FROM role_permissions WHERE role='USER' AND permission_id=(SELECT id FROM permissions WHERE name='trip:read')");
        try {
            mvc.perform(get("/api/v1/trips").header("Authorization","Bearer "+memberToken)).andExpect(status().isForbidden());
        } finally {
            jdbc.update("INSERT INTO role_permissions(role,permission_id) SELECT 'USER',id FROM permissions WHERE name='trip:read' ON CONFLICT DO NOTHING");
        }
    }
    @Test void concurrentCrossDemotionsCannotRemoveAllAdministrators() throws Exception {
        // Each can demote the other; serialization rechecks the actor after the winner commits.
        var other=account(Role.ADMIN);String otherToken=token(other);
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(() -> {start.await();return change(other,Role.USER,false,0,adminToken).andReturn().getResponse().getStatus();});
            var b=pool.submit(() -> {start.await();return change(admin,Role.USER,false,0,otherToken).andReturn().getResponse().getStatus();});
            start.countDown();var statuses=List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));
            assertThat(statuses).contains(200);assertThat(statuses.stream().filter(s->s==200).count()).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id IN (?,?) AND role='ADMIN' AND locked_at IS NULL",Integer.class,admin.getId(),other.getId())).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    ResultActions change(User target, Role role, boolean locked, long version, String actorToken) throws Exception {
        return mvc.perform(patch("/api/v1/admin/users/"+target.getId()).header("Authorization","Bearer "+actorToken)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("role",role,"locked",locked,"version",version))));
    }

    @Test void publicCatalogNeverExposesPrivateUnapprovedOrUnverifiedDemoPlaces() throws Exception {
        UUID province=province();
        UUID published=place(province,"INTERNAL",true,"EXISTENCE_ONLY");
        UUID privatePlace=place(province,"USER",true,"EXISTENCE_ONLY");
        UUID unapproved=place(province,"INTERNAL",false,"EXISTENCE_ONLY");
        UUID demo=place(province,"INTERNAL",true,"DEV_DEMO_ONLY_NOT_FACT_VERIFIED");
        mvc.perform(get("/api/v1/public/places/"+published)).andExpect(status().isOk());
        for(UUID id:List.of(privatePlace,unapproved,demo))
            mvc.perform(get("/api/v1/public/places/"+id)).andExpect(status().isNotFound());
        var response=mvc.perform(get("/api/v1/public/places").param("provinceId",province.toString()).param("q","RBAC catalog"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(response).path("data").path("content")).hasSize(1);
    }
    @Test void catalogApprovalRequiresAdminAndRejectsStaleOrExplicitlyUnverifiedData() throws Exception {
        UUID province=province();
        UUID id=place(province,"INTERNAL",false,"EXISTENCE_ONLY");
        mvc.perform(patch("/api/v1/admin/catalog/"+id).header("Authorization","Bearer "+memberToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true,\"version\":1}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/admin/catalog/"+id).header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true,\"version\":1}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/places/"+id)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/admin/catalog/"+id).header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":false,\"version\":1}")).andExpect(status().isConflict());
        UUID demo=place(province,"INTERNAL",false,"DEV_DEMO_ONLY_NOT_FACT_VERIFIED");
        mvc.perform(patch("/api/v1/admin/catalog/"+demo).header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true,\"version\":1}")).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_catalog_events WHERE place_id=?",Integer.class,id)).isEqualTo(1);
    }
    UUID province() {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO provinces(id,code,name) VALUES (?,?,?)",id,"R"+id.toString().substring(0,8),"RBAC fixture province");
        return id;
    }
    UUID place(UUID province,String source,boolean approved,String scope) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO places(id,name,latitude,longitude,province_id,source,owner_id,place_type) VALUES (?,'RBAC catalog fixture',16.06,108.22,?,?,?,'OUTDOOR')",
                id,province,source,source.equals("USER")?member.getId():null);
        jdbc.update("INSERT INTO place_retrieval_profiles(place_id,description,tags,estimated_duration_minutes,source_url,reviewed_on,verification_scope,approved) VALUES (?,'Fixture',ARRAY['HISTORY'],30,'https://example.com',CURRENT_DATE,?,?)",id,scope,approved);
        return id;
    }
}
