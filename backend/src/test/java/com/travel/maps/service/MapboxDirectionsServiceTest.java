package com.travel.maps.service;

import static com.travel.maps.service.MapboxDirectionsService.Mode.DRIVE;
import static com.travel.maps.service.MapboxDirectionsService.Mode.WALK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.maps.dto.RoutePoint;
import com.travel.maps.dto.TripDayRoute;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MapboxDirectionsServiceTest {

    private final RestClient.Builder client = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
    private final MapboxDirectionsService service = new MapboxDirectionsService(
        "test-token",
        new ObjectMapper(),
        client.build()
    );
    private final List<RoutePoint> points = List.of(
        new RoutePoint(null, 16.0, 108.0),
        new RoutePoint(null, 16.1, 108.1),
        new RoutePoint(null, 16.2, 108.2),
        new RoutePoint(null, 16.3, 108.3)
    );

    @Test
    void decodesOrderedLegsAndProviderMetrics() {
        server
            .expect(request ->
                assertThat(request.getURI().getPath()).isEqualTo("/directions/v5/mapbox/walking/108.0,16.0;108.1,16.1")
            )
            .andExpect(queryParam("geometries", "polyline"))
            .andRespond(
                withSuccess(
                    """
                    {"code":"Ok","routes":[{"geometry":"_p~iF~ps|U_ulLnnqC","distance":1240.3,"duration":840,
                    "legs":[{"distance":1240.3,"duration":840}]}]}
                    """,
                    MediaType.APPLICATION_JSON
                )
            );

        var result = service.route(points.subList(0, 2), WALK);
        assertThat(result.status()).isEqualTo(TripDayRoute.Status.READY);
        assertThat(result.route().distanceMeters()).isEqualTo(1240.3);
        assertThat(result.route().legs()).containsExactly(new TripDayRoute.Leg(1240.3, 840));
        server.verify();
    }

    @Test
    void missingCoordinatesAndMissingRouteUseTypedFallbacks() {
        assertThat(
            service.route(List.of(points.getFirst(), new RoutePoint(null, null, 108.0)), DRIVE).status()
        ).isEqualTo(TripDayRoute.Status.MISSING_COORDINATES);
        server
            .expect(queryParam("steps", "false"))
            .andRespond(withSuccess("{\"code\":\"NoRoute\"}", MediaType.APPLICATION_JSON));
        assertThat(service.route(points, DRIVE).status()).isEqualTo(TripDayRoute.Status.NO_ROUTE);
        server.verify();
    }

    @Test
    void rejectsInvalidLegCount() {
        server.expect(queryParam("steps", "false")).andRespond(
            withSuccess(
                """
                {"code":"Ok","routes":[{"geometry":"test","distance":120,"duration":90,"legs":[]}]}
                """,
                MediaType.APPLICATION_JSON
            )
        );
        assertThatThrownBy(() -> service.route(points, DRIVE)).isInstanceOf(AppException.class);
        server.verify();
    }

    @Test
    void matrixReadsDirectedRoadDurationsAndDistancesInOneRequest() {
        server
            .expect(request ->
                assertThat(request.getURI().getPath()).startsWith("/directions-matrix/v1/mapbox/driving/")
            )
            .andExpect(queryParam("annotations", "duration,distance"))
            .andRespond(
                withSuccess(
                    """
                    {"code":"Ok","durations":[[0,20,5],[40,0,6],[8,9,0]],
                     "distances":[[0,200,50],[400,0,60],[80,90,0]]}
                    """,
                    MediaType.APPLICATION_JSON
                )
            );
        var matrix = service.matrix(points.subList(0, 3), DRIVE);
        assertThat(matrix.durations()[0][1]).isEqualTo(20);
        assertThat(matrix.durations()[1][0]).isEqualTo(40);
        assertThat(matrix.distances()[2][1]).isEqualTo(90);
        server.verify();
    }

    @Test
    void matrixKeepsUnreachablePairsUnknownInsteadOfInventingTravelTimes() {
        server
            .expect(queryParam("annotations", "duration,distance"))
            .andRespond(
                withSuccess(
                    "{\"code\":\"Ok\",\"durations\":[[0,null],[7,0]],\"distances\":[[0,null],[70,0]]}",
                    MediaType.APPLICATION_JSON
                )
            );
        assertThat(service.matrix(points.subList(0, 2), DRIVE).durations()[0][1]).isInfinite();
        server.verify();
    }

    @Test
    void matrixRejectsWrongDimensionsAndNegativeDurations() {
        server
            .expect(queryParam("annotations", "duration,distance"))
            .andRespond(
                withSuccess(
                    "{\"code\":\"Ok\",\"durations\":[[0,-1],[7,0]],\"distances\":[[0,2],[3,0]]}",
                    MediaType.APPLICATION_JSON
                )
            );
        assertThatThrownBy(() -> service.matrix(points.subList(0, 2), DRIVE)).isInstanceOf(AppException.class);
        server.verify();
    }
}
