package com.travel.place.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.travel.common.exception.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MapboxAddressServiceTest {

    MockRestServiceServer server;
    MapboxAddressService service;
    static final String RESPONSE = """
    {"type":"FeatureCollection","attribution":"© Mapbox","features":[{"type":"Feature",
    "geometry":{"type":"Point","coordinates":[106.663,10.838]},"properties":{"mapbox_id":"dXJuOm1ieGFkcjo123",
    "feature_type":"address","name":"100/25 Quang Trung","full_address":"100/25 Quang Trung, Gò Vấp, Việt Nam"}}]}
    """;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new MapboxAddressService(
            true,
            "test-token",
            builder.build(),
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void sendsBoundedVietnameseTemporaryQueryAndPreservesCoordinateOrderAndAttribution() {
        server
            .expect(method(HttpMethod.GET))
            .andExpect(queryParam("q", "100/25%20Quang%20Trung"))
            .andExpect(queryParam("country", "vn"))
            .andExpect(queryParam("language", "vi"))
            .andExpect(queryParam("permanent", "false"))
            .andExpect(queryParam("limit", "5"))
            .andExpect(queryParam("proximity", "106.663,10.838"))
            .andRespond(withSuccess(RESPONSE, MediaType.APPLICATION_JSON));
        var result = service.search(" 100/25 Quang Trung ", 10.838, 106.663, "vi");
        assertThat(result.storage()).isEqualTo("TEMPORARY");
        assertThat(result.provider()).isEqualTo("MAPBOX");
        assertThat(result.attribution()).isEqualTo("© Mapbox");
        assertThat(result.items().getFirst().location().latitude()).isEqualTo(10.838);
        assertThat(result.items().getFirst().location().longitude()).isEqualTo(106.663);
        assertThat(result.items().getFirst().providerPlaceId()).startsWith("dXJu");
    }

    @Test
    void emptyFeatureCollectionIsValidNotAProviderFailure() {
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));
        assertThat(service.search("nothing", null, null, "en").items()).isEmpty();
    }

    @Test
    void missingAndInvalidCoordinatesAreNotReplacedWithZero() {
        server
            .expect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(RESPONSE.replace("[106.663,10.838]", "[106.663,100.838]"), MediaType.APPLICATION_JSON)
            );
        assertThatThrownBy(() -> service.search("address", null, null, "en")).isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_PROVIDER_INVALID_RESPONSE)
        );
    }

    @Test
    void malformedProviderShapeIsNotAnEmptySuccess() {
        server.expect(method(HttpMethod.GET)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.search("address", null, null, "en")).isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_PROVIDER_INVALID_RESPONSE)
        );
    }

    @Test
    void quotaAndForbiddenDoNotLeakTokenOrProviderMessage() {
        server
            .expect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("test-token sensitive-provider-error"));
        assertThatThrownBy(() -> service.search("address", null, null, "en")).isInstanceOfSatisfying(
            AppException.class,
            e -> {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_PROVIDER_UNAVAILABLE);
                assertThat(e.getMessage()).doesNotContain("test-token", "sensitive");
            }
        );
    }

    @Test
    void disabledProviderDoesNotPerformHttpCall() {
        var disabled = new MapboxAddressService(
            false,
            "",
            RestClient.create(),
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
        assertThatThrownBy(() -> disabled.search("address", null, null, "vi")).isInstanceOfSatisfying(
            AppException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_PROVIDER_UNAVAILABLE)
        );
    }

    @Test
    void invalidQueriesAreRejectedBeforeHttpAndEnabledProviderRequiresToken() {
        assertThatThrownBy(() -> service.search("ab", null, null, "vi")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.search("address", 10.0, null, "vi")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.search("address", Double.NaN, 106.0, "vi")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.search("address", null, null, "fr")).isInstanceOf(AppException.class);
        assertThatThrownBy(() ->
            new MapboxAddressService(true, "", RestClient.create(), new com.fasterxml.jackson.databind.ObjectMapper())
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
