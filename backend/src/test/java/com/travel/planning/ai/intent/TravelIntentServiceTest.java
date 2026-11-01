package com.travel.planning.ai.intent;

import com.travel.planning.retrieval.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TravelIntentServiceTest {
    GeminiIntentClient client = mock(GeminiIntentClient.class);
    PlaceRetrievalService retrieval = mock(PlaceRetrievalService.class);
    TravelIntentService service = new TravelIntentService(client, retrieval, new RagDemoProperties());
    PlaceCandidate museum = place("Bảo tàng Đà Nẵng", List.of("MUSEUM", "HISTORY"));
    PlaceCandidate beach = place("Bãi biển Mỹ Khê", List.of("BEACH", "SCENERY"));

    @BeforeEach void catalog() { when(retrieval.catalog()).thenReturn(List.of(museum, beach)); }

    @Test void missingDestinationIsNotAssumedToBeDaNang() {
        when(client.extract("text")).thenReturn(IntentFixtures.intent(null, List.of("ART"), List.of(), List.of(), List.of()));
        var result = service.search("text", 4);
        assertThat(result.status()).isEqualTo("NEEDS_INPUT"); assertThat(result.candidates()).isNull();
        assertThat(result.analysis().intent().destination()).isNull();
        assertThat(result.analysis().missingFields()).contains("destination", "numberOfDays", "dailyStartTime");
        verify(retrieval, never()).search(anyString(), anyList(), anyInt());
    }

    @Test void unsupportedProvinceDoesNotSearchDaNang() {
        when(client.extract("text")).thenReturn(IntentFixtures.intent("Huế", List.of("HISTORY"), List.of(), List.of(), List.of()));
        assertThat(service.search("text", 4).status()).isEqualTo("UNSUPPORTED_DESTINATION");
        verify(retrieval, never()).search(anyString(), anyList(), anyInt());
    }

    @Test void unresolvableMustVisitDoesNotBecomeAnInventedIdOrAlternative() {
        when(client.extract("text")).thenReturn(IntentFixtures.intent("Đà Nẵng", List.of("ART"), List.of(), List.of("ZZZ không tồn tại"), List.of()));
        var result = service.search("text", 4);
        assertThat(result.status()).isEqualTo("NEEDS_INPUT"); assertThat(result.candidates()).isNull();
        assertThat(result.analysis().mustVisitPlaces()).isEmpty();
    }

    @Test void ambiguousNameRequiresConfirmation() {
        when(retrieval.catalog()).thenReturn(List.of(museum, place("Bảo tàng Mỹ thuật Đà Nẵng", List.of("MUSEUM"))));
        when(client.extract("text")).thenReturn(IntentFixtures.intent("Da Nang", List.of(), List.of(), List.of("bảo tàng"), List.of()));
        assertThat(service.search("text", 4).status()).isEqualTo("NEEDS_INPUT");
    }

    @Test void contradictoryInterestsAreNotSilentlyRelaxed() {
        when(client.extract("text")).thenReturn(IntentFixtures.intent("Đà Nẵng", List.of("BEACH"), List.of("BEACH"), List.of(), List.of()));
        assertThat(service.search("text", 4).status()).isEqualTo("NEEDS_INPUT");
    }

    @Test void contradictoryRequiredPlaceAndExcludedGroupRequiresConfirmation() {
        when(client.extract("text")).thenReturn(IntentFixtures.intent("Đà Nẵng", List.of("HISTORY"), List.of("BEACH"), List.of("Mỹ Khê"), List.of()));
        assertThat(service.search("text", 4).status()).isEqualTo("NEEDS_INPUT");
    }

    @Test void invalidTimeWindowRequiresConfirmation() {
        when(client.extract("text")).thenReturn(new TravelIntent("Đà Nẵng", 2, List.of("ART"), List.of(),
                "18:00", "08:00", null, List.of(), List.of(), Map.of()));
        assertThat(service.search("text", 4).status()).isEqualTo("NEEDS_INPUT");
    }

    @Test void requiredPlaceUsesDatabaseIdAndIsKeptEvenWithNoRankedMatches() {
        when(client.extract("text")).thenReturn(IntentFixtures.intent("Da Nang", List.of(), List.of(), List.of("Mỹ Khê"), List.of()));
        when(retrieval.search(anyString(), anyList(), eq(8))).thenReturn(new PlaceRetrievalService.SearchResult("LEXICAL", "fixture", List.of()));
        var result = service.search("text", 1);
        assertThat(result.status()).isEqualTo("CANDIDATES_FOUND");
        assertThat(result.candidates().matches()).extracting(m -> m.place().placeId()).containsExactly(beach.placeId());
        assertThat(result.analysis().missingFields()).contains("numberOfDays");
    }

    @Test void excludesSpecificNameWithoutExcludingItsWholeCategory() {
        when(client.extract("text")).thenReturn(IntentFixtures.intent("Đà Nẵng", List.of("SCENERY"), List.of(), List.of(), List.of("Mỹ Khê")));
        when(retrieval.search(anyString(), anyList(), eq(8))).thenReturn(new PlaceRetrievalService.SearchResult("fixture", "fixture", List.of(
                new PlaceRetrievalService.Match(beach, .03, true, true), new PlaceRetrievalService.Match(museum, .02, true, true))));
        var result = service.search("text", 4);
        assertThat(result.candidates().matches()).extracting(m -> m.place().placeId()).containsExactly(museum.placeId());
        assertThat(result.analysis().intent().excludedTags()).isEmpty();
    }

    private PlaceCandidate place(String name, List<String> tags) {
        return new PlaceCandidate(UUID.randomUUID(), name, "description", tags, 16, 108, "UNSPECIFIED", null, null,
                60, "https://example.com/source", LocalDate.of(2026, 10, 30), "DESCRIPTION_ONLY", "APPROXIMATE_DEMO", "UNKNOWN", 1);
    }
}
