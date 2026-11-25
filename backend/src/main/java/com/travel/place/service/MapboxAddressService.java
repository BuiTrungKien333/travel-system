package com.travel.place.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.place.dto.PlaceDiscovery.Location;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Temporary address preview; no POI enrichment, persistence, shared cache, or automatic retry.
 */
@Service
public class MapboxAddressService {

    private final boolean enabled;
    private final String token;
    private final RestClient client;
    private final ObjectMapper json;

    @Autowired
    public MapboxAddressService(
        @Value("${application.maps.mapbox.geocoding-enabled:false}") boolean enabled,
        @Value("${application.maps.mapbox.access-token:}") String token,
        @Value("${application.maps.mapbox.timeout-seconds:5}") int timeout,
        ObjectMapper json
    ) {
        this(enabled, token, client(timeout), json);
    }

    MapboxAddressService(boolean enabled, String token, RestClient client, ObjectMapper json) {
        if (enabled && (token == null || token.isBlank())) {
            throw new IllegalArgumentException("Configure MAPBOX_ACCESS_TOKEN before enabling geocoding");
        }
        this.enabled = enabled;
        this.token = token;
        this.client = client;
        this.json = json;
    }

    private static RestClient client(int timeout) {
        if (timeout < 1 || timeout > 10) {
            throw new IllegalArgumentException("Mapbox timeout must be 1–10 seconds");
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeout));
        factory.setReadTimeout(Duration.ofSeconds(timeout));
        return RestClient.builder().requestFactory(factory).build();
    }

    public record Address(
        String providerPlaceId,
        String name,
        String formattedAddress,
        Location location,
        String featureType
    ) {}

    public record Results(List<Address> items, String provider, String storage, String attribution) {}

    public Results search(String query, Double latitude, Double longitude, String locale) {
        String q = query == null ? "" : query.trim();
        if (
            q.length() < 3 ||
            q.length() > 200 ||
            !Set.of("vi", "en").contains(locale) ||
            (latitude == null) != (longitude == null) ||
            (latitude != null &&
                (!Double.isFinite(latitude) ||
                    Math.abs(latitude) > 90 ||
                    !Double.isFinite(longitude) ||
                    Math.abs(longitude) > 180))
        ) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        if (!enabled) {
            throw new AppException(ErrorCode.PLACE_PROVIDER_UNAVAILABLE);
        }

        var uri = UriComponentsBuilder.fromUriString("https://api.mapbox.com/search/geocode/v6/forward")
            .queryParam("q", q)
            .queryParam("country", "vn")
            .queryParam("language", locale)
            .queryParam("limit", 5)
            .queryParam("autocomplete", true)
            .queryParam("permanent", false)
            .queryParam("access_token", token);

        if (latitude != null) {
            uri.queryParam("proximity", longitude + "," + latitude);
        }

        JsonNode result;
        try {
            // Spring 7's default RestClient uses Jackson 3. Parse through our application's Jackson 2 mapper.
            String payload = client.get().uri(uri.build().encode().toUri()).retrieve().body(String.class);
            if (payload == null || payload.length() > 131072) {
                invalid();
            }
            result = json.readTree(payload);
        } catch (JsonProcessingException error) {
            throw new AppException(ErrorCode.PLACE_PROVIDER_INVALID_RESPONSE);
        } catch (RestClientException error) {
            // Never propagate a URL (which contains the access token), provider body, or stacktrace.
            throw new AppException(
                error instanceof ResourceAccessException || error instanceof RestClientResponseException
                    ? ErrorCode.PLACE_PROVIDER_UNAVAILABLE
                    : ErrorCode.PLACE_PROVIDER_INVALID_RESPONSE
            );
        }

        if (result == null || !result.isObject() || !result.path("features").isArray()) {
            invalid();
        }

        List<Address> items = new ArrayList<>();
        for (JsonNode feature : result.path("features")) {
            var properties = feature.path("properties");
            var coordinates = feature.path("geometry").path("coordinates");
            String id = text(properties, "mapbox_id");
            String name = text(properties, "name");
            String address = text(properties, "full_address");

            if (address == null) {
                String context = text(properties, "place_formatted");
                address = context == null ? name : name == null ? context : name + ", " + context;
            }

            if (
                id == null ||
                name == null ||
                name.length() > 255 ||
                id.length() > 512 ||
                (address != null && address.length() > 2000) ||
                !"Point".equals(text(feature.path("geometry"), "type")) ||
                !coordinates.isArray() ||
                coordinates.size() != 2 ||
                !coordinates.get(0).isNumber() ||
                !coordinates.get(1).isNumber()
            ) {
                invalid();
            }

            double lng = coordinates.get(0).doubleValue();
            double lat = coordinates.get(1).doubleValue();
            if (!Double.isFinite(lat) || Math.abs(lat) > 90 || !Double.isFinite(lng) || Math.abs(lng) > 180) {
                invalid();
            }

            items.add(new Address(id, name, address, new Location(lat, lng), text(properties, "feature_type")));
            if (items.size() == 5) {
                break;
            }
        }

        return new Results(List.copyOf(items), "MAPBOX", "TEMPORARY", text(result, "attribution"));
    }

    private static String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static void invalid() {
        throw new AppException(ErrorCode.PLACE_PROVIDER_INVALID_RESPONSE);
    }
}
