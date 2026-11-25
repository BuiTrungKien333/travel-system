package com.travel.place.service;

import com.travel.common.exception.*;
import com.travel.place.dto.*;
import com.travel.place.repository.*;
import com.travel.trip.repository.TripRepository;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class PlaceDiscoveryService {

    private final PlaceDiscoveryRepository catalog;
    private final PlaceRepository places;
    private final ProvinceRepository provinces;
    private final TripRepository trips;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PlaceDiscovery.Page suggestions(
        UUID owner,
        UUID tripId,
        UUID provinceId,
        String query,
        int page,
        int size,
        String locale
    ) {
        String text = query == null ? "" : query.trim();
        if (
            page < 0 ||
            size < 1 ||
            size > 100 ||
            text.length() > 200 ||
            !Set.of("vi", "en").contains(locale) ||
            (tripId != null && provinceId != null)
        ) throw new AppException(ErrorCode.VALIDATION_ERROR);
        UUID province = provinceId;
        if (tripId != null) province = trips
            .findByIdAndUserIdAndDeletedAtIsNull(tripId, owner)
            .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND))
            .getDestinationProvinceId();
        else if (provinceId != null && !provinces.existsById(provinceId)) throw new AppException(
            ErrorCode.PROVINCE_NOT_FOUND
        );
        Instant now = clock.instant();
        var context = new DestinationPage.Context(now, LocalDate.ofInstant(now, ZoneId.of("Asia/Ho_Chi_Minh")));
        if (province == null) return new PlaceDiscovery.Page(
            List.of(),
            page,
            size,
            0,
            0,
            "DESTINATION_NOT_LINKED",
            context
        );
        var filter = catalog.filter(province, tripId, text);
        long total = catalog.count(filter);
        return new PlaceDiscovery.Page(
            catalog.page(filter, size, (long) page * size),
            page,
            size,
            total,
            (total + size - 1) / size,
            total == 0 ? "EMPTY_CATALOG" : null,
            context
        );
    }

    @Transactional(readOnly = true)
    public PlaceDiscovery.Detail publicDetail(UUID id, String locale) {
        if (!Set.of("vi", "en").contains(locale)) throw new AppException(ErrorCode.VALIDATION_ERROR);
        if (!catalog.isPublished(id)) throw new AppException(ErrorCode.PLACE_NOT_FOUND);
        return PlaceDiscovery.detail(
            places.findVisible(id, null).orElseThrow(() -> new AppException(ErrorCode.PLACE_NOT_FOUND)),
            catalog.samplePhotoUrls(id),
            clock.instant()
        );
    }

    @Transactional(readOnly = true)
    public PlaceDiscovery.Detail detail(UUID owner, UUID id, String locale) {
        if (!Set.of("vi", "en").contains(locale)) throw new AppException(ErrorCode.VALIDATION_ERROR);
        return PlaceDiscovery.detail(
            places.findVisible(id, owner).orElseThrow(() -> new AppException(ErrorCode.PLACE_NOT_FOUND)),
            catalog.samplePhotoUrls(id),
            clock.instant()
        );
    }
}
