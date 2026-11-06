package com.travel.place.service;

import com.travel.place.dto.DestinationPage;
import com.travel.place.repository.DestinationCatalogRepository;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DestinationCatalogService {
    private final DestinationCatalogRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DestinationPage list(String query, int page, int size) {
        String text = query == null ? "" : query.trim();
        if (page < 0 || size < 1 || size > 100 || text.length() > 120)
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    Map.of("query", "Invalid catalog query or page"));
        var search = repository.search(text);
        long total = repository.count(search);
        Instant now = clock.instant();
        return new DestinationPage(
                repository.page(search, size, (long) page * size),
                page, size, total, (total + size - 1) / size,
                new DestinationPage.Context(now, LocalDate.ofInstant(now, ZoneId.of("Asia/Ho_Chi_Minh"))));
    }
}
