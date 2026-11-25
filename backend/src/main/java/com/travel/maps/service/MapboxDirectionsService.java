package com.travel.maps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.maps.dto.RoutePoint;
import com.travel.maps.dto.TripDayRoute;
import com.travel.maps.dto.TripDayRoute.Leg;
import com.travel.maps.dto.TripDayRoute.Route;
import com.travel.maps.dto.TripDayRoute.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.stream.Collectors;

@Service
public class MapboxDirectionsService {
    public record RoadMatrix(double[][] distances, double[][] durations) { }
    public enum Mode {
        DRIVE("driving"), WALK("walking"), BICYCLE("cycling");
        private final String profile;
        Mode(String profile) { this.profile = profile; }
    }

    private final String token;
    private final ObjectMapper json;
    private final RestClient client;

    @Autowired
    public MapboxDirectionsService(@Value("${application.maps.mapbox.access-token:}") String token,
            @Value("${application.maps.mapbox.timeout-seconds:5}") int timeout, ObjectMapper json) {
        this(token, json, httpClient(timeout));
    }

    MapboxDirectionsService(String token, ObjectMapper json, RestClient client) {
        this.token = token == null ? "" : token.trim();
        this.json = json;
        this.client = client;
    }

    private static RestClient httpClient(int timeout) {
        var factory = new SimpleClientHttpRequestFactory();
        var duration = Duration.ofSeconds(Math.clamp(timeout, 1, 10));
        factory.setConnectTimeout(duration);
        factory.setReadTimeout(duration);
        return RestClient.builder().requestFactory(factory).build();
    }

    public TripDayRoute route(List<RoutePoint> points, Mode mode) {
        if (points.size() < 2) return TripDayRoute.unavailable(Status.TOO_FEW_STOPS);
        if (points.size() > 25) return TripDayRoute.unavailable(Status.TOO_MANY_STOPS);
        if (points.stream().anyMatch(point -> !valid(point))) return TripDayRoute.unavailable(Status.MISSING_COORDINATES);
        if (token.isBlank()) return TripDayRoute.unavailable(Status.NOT_CONFIGURED);

        var uri = uri("directions/v5", points, mode)
                .queryParam("geometries", "polyline").queryParam("overview", "simplified")
                .queryParam("steps", false)
                .build().encode().toUri();
        JsonNode response = request(uri);
        String code = response.path("code").asText();
        if ("NoRoute".equals(code) || "NoSegment".equals(code)) return TripDayRoute.unavailable(Status.NO_ROUTE);
        if (!"Ok".equals(code)) throw invalid();

        var first = response.path("routes").path(0);
        var geometry = first.path("geometry");
        var rawLegs = first.path("legs");
        if (!geometry.isTextual() || geometry.asText().isBlank() || geometry.asText().length() > 250_000
                || !rawLegs.isArray() || rawLegs.size() != points.size() - 1) throw invalid();
        List<Leg> legs = new ArrayList<>(rawLegs.size());
        for (JsonNode leg : rawLegs) legs.add(new Leg(metric(leg, "distance"), metric(leg, "duration")));
        return new TripDayRoute(Status.READY, new Route(metric(first, "distance"), metric(first, "duration"),
                geometry.asText(), List.copyOf(legs)));
    }

    public RoadMatrix matrix(List<RoutePoint> points, Mode mode) {
        if (points.size() < 2 || points.size() > 12 || points.stream().anyMatch(point -> !valid(point)))
            throw new AppException(ErrorCode.INVALID_ITINERARY);
        if (token.isBlank()) throw new AppException(ErrorCode.PLACE_PROVIDER_UNAVAILABLE);
        var response = request(uri("directions-matrix/v1", points, mode)
                .queryParam("annotations", "duration,distance").build().encode().toUri());
        if ("NoRoute".equals(response.path("code").asText()))
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        if (!"Ok".equals(response.path("code").asText())) throw invalid();
        return new RoadMatrix(matrixValues(response.path("distances"), points.size()),
                matrixValues(response.path("durations"), points.size()));
    }

    public void checkConnection() {
        matrix(List.of(new RoutePoint("A", 16.0544, 108.2022),
                new RoutePoint("B", 16.0611, 108.2275)), Mode.DRIVE);
    }

    private double[][] matrixValues(JsonNode rows, int size) {
        if (!rows.isArray() || rows.size() != size) throw invalid();
        double[][] result = new double[size][size];
        for (int from = 0; from < size; from++) {
            var row = rows.get(from);
            if (!row.isArray() || row.size() != size) throw invalid();
            for (int to = 0; to < size; to++) {
                var value = row.get(to);
                if (value.isNull()) result[from][to] = Double.POSITIVE_INFINITY;
                else {
                    if (!value.isNumber() || !Double.isFinite(value.asDouble()) || value.asDouble() < 0) throw invalid();
                    result[from][to] = value.asDouble();
                }
            }
        }
        return result;
    }

    private UriComponentsBuilder uri(String endpoint, List<RoutePoint> points, Mode mode) {
        String coordinates = points.stream().map(p -> p.longitude() + "," + p.latitude()).collect(Collectors.joining(";"));
        return UriComponentsBuilder.fromUriString("https://api.mapbox.com/" + endpoint + "/mapbox")
                .pathSegment(mode.profile, coordinates).queryParam("access_token", token);
    }

    private JsonNode request(URI uri) {
        try {
            String payload = client.get().uri(uri).retrieve().body(String.class);
            if (payload == null || payload.length() > 524_288) throw invalid();
            JsonNode result = json.readTree(payload);
            if (result == null) throw invalid();
            return result;
        } catch (JsonProcessingException error) {
            throw invalid();
        } catch (RestClientException error) {
            throw new AppException(ErrorCode.PLACE_PROVIDER_UNAVAILABLE);
        }
    }

    private static boolean valid(RoutePoint point) {
        return point.latitude() != null && point.longitude() != null
                && Double.isFinite(point.latitude()) && Math.abs(point.latitude()) <= 85.0511
                && Double.isFinite(point.longitude()) && Math.abs(point.longitude()) <= 180;
    }

    private static double metric(JsonNode parent, String name) {
        var node = parent.path(name);
        double value = node.asDouble();
        if (!node.isNumber() || !Double.isFinite(value) || value < 0) throw invalid();
        return value;
    }

    private static AppException invalid() {
        return new AppException(ErrorCode.PLACE_PROVIDER_INVALID_RESPONSE);
    }
}
