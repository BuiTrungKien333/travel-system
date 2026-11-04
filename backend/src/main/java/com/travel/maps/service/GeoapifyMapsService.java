package com.travel.maps.service;

import tools.jackson.databind.JsonNode;
import com.travel.maps.dto.PlaceSearchResult;
import com.travel.maps.dto.RoutePoint;
import com.travel.maps.dto.RouteRequest;
import com.travel.maps.dto.RouteResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeoapifyMapsService {
    private final String apiKey;
    private final RestClient client;

    public GeoapifyMapsService(
            @Value("${application.maps.geoapify.api-key:}") String apiKey,
            @Value("${application.maps.geoapify.timeout-seconds:10}") int timeoutSeconds) {
        this.apiKey = apiKey;
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
    }

    public List<PlaceSearchResult> search(String query, Double latitude, Double longitude, Integer radiusMeters) {
        requireConfigured();
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString("https://api.geoapify.com/v1/geocode/search")
                .queryParam("text", query.trim())
                .queryParam("format", "json")
                .queryParam("limit", 15)
                .queryParam("lang", "vi")
                .queryParam("filter", "countrycode:vn")
                .queryParam("apiKey", apiKey);
        if (latitude != null && longitude != null) {
            String bias = radiusMeters == null
                    ? "proximity:" + longitude + "," + latitude
                    : "circle:" + longitude + "," + latitude + "," + radiusMeters;
            uri.queryParam("bias", bias);
        }
        JsonNode response = client.get().uri(uri.build().encode().toUri()).retrieve().body(JsonNode.class);
        List<PlaceSearchResult> results = new ArrayList<>();
        JsonNode matches = response == null ? null : response.path("results");
        if (matches != null && matches.isArray()) {
            for (JsonNode match : matches) {
                String name = text(match, "name");
                if (!StringUtils.hasText(name)) name = text(match, "address_line1");
                if (!StringUtils.hasText(name)) name = text(match, "formatted");
                results.add(new PlaceSearchResult(text(match, "place_id"), name,
                        text(match, "formatted"), number(match, "lat"), number(match, "lon"),
                        text(match, "result_type")));
            }
        }
        return List.copyOf(results);
    }

    public RouteResponse route(RouteRequest request) {
        requireConfigured();
        List<RoutePoint> points = new ArrayList<>();
        points.add(request.origin());
        if (request.intermediates() != null) points.addAll(request.intermediates());
        points.add(request.destination());
        String waypoints = points.stream()
                .map(point -> point.latitude() + "," + point.longitude())
                .collect(java.util.stream.Collectors.joining("|"));
        JsonNode response = client.get().uri(UriComponentsBuilder
                        .fromUriString("https://api.geoapify.com/v1/routing")
                        .queryParam("waypoints", waypoints)
                        .queryParam("mode", toGeoapifyMode(request.travelMode()))
                        .queryParam("details", "polyline5")
                        .queryParam("format", "json")
                        .queryParam("units", "metric")
                        .queryParam("apiKey", apiKey).build().encode().toUri())
                .retrieve().body(JsonNode.class);
        JsonNode route = response == null ? null : response.path("results").path(0);
        if (route == null || route.isMissingNode())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No route found for these points");
        return new RouteResponse(route.path("distance").asInt(), route.path("time").asLong(),
                text(route, "polyline5"), "geoapify-routing");
    }

    private void requireConfigured() {
        if (!StringUtils.hasText(apiKey))
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Places and routing are not configured");
    }

    private static String toGeoapifyMode(RouteRequest.TravelMode mode) {
        if (mode == null) return "drive";
        return switch (mode) {
            case DRIVE -> "drive";
            case WALK -> "walk";
            case BICYCLE -> "bicycle";
            case TWO_WHEELER -> "motorcycle";
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
    private static Double number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }
}
