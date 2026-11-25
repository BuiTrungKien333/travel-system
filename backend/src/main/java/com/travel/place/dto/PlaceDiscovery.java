package com.travel.place.dto;

import com.travel.place.entity.Place;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/** Discovery identity is separate from the trip-place membership identity. */
public final class PlaceDiscovery {
    private PlaceDiscovery() { }
    public record Reference(String kind, UUID placeId) { }
    public record Location(double latitude, double longitude) { }
    public record Category(String code, String label) { }
    public record Summary(UUID id, Reference reference, String source, String name, String formattedAddress,
                          Location location, UUID provinceId, Category category, Photo photo) { }
    public record Page(List<Summary> content, int page, int size, long totalElements, long totalPages,
                       String reason, DestinationPage.Context context) { }
    public record Hours(List<String> displayLines, String source) { }
    public record Availability(String hours, String admission, String photos, String description) { }
    public record Attribution(String kind, String label, String url) { }
    public record Photo(String url, String source, List<Attribution> attributions) { }
    public record Admission(BigDecimal amount, String currency, String text, String source) { }
    public record Detail(UUID id, Reference reference, String source, String name, String formattedAddress,
                         Location location, UUID provinceId, Category category, String environmentType,
                         Hours openingHours, String openingStatus, String businessStatus, Instant openingStatusAsOf,
                         Admission admission, List<String> amenities, List<String> tips, String description,
                         List<Photo> photos, List<Attribution> attributions, Availability dataAvailability, Instant evaluatedAt) { }

    public static Detail detail(Place p, List<String> samplePhotoUrls, Instant now) {
        Hours hours = p.getOpeningTime() != null && p.getClosingTime() != null
                ? new Hours(List.of(p.getOpeningTime() + " – " + p.getClosingTime()), "INTERNAL") : null;
        List<Photo> photos = samplePhotoUrls.stream().map(url ->
                new Photo(url, "ADMIN_SAMPLE", List.of())).toList();
        return new Detail(p.getId(), new Reference(p.getSource(), p.getId()), p.getSource(), p.getName(), p.getAddress(),
                new Location(p.getLatitude(), p.getLongitude()), p.getProvince() == null ? null : p.getProvince().getId(),
                p.getCategory() == null ? null : new Category(p.getCategory(), p.getCategory()), p.getPlaceType().name(),
                hours, "UNKNOWN", "UNKNOWN", null, null, List.of(), List.of(), null, photos, List.of(),
                new Availability(hours == null ? "NOT_PROVIDED" : "AVAILABLE", "NOT_PROVIDED",
                        photos.isEmpty() ? "NOT_PROVIDED" : "AVAILABLE", "NOT_PROVIDED"), now);
    }
}
