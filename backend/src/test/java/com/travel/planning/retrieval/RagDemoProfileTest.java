package com.travel.planning.retrieval;

import com.travel.planning.ai.intent.controller.TravelIntentController;
import com.travel.planning.draft.controller.DraftController;
import com.travel.planning.retrieval.controller.PlaceIndexController;
import com.travel.planning.retrieval.controller.PlaceRetrievalController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import com.travel.planning.ai.intent.service.TravelIntentService;
import com.travel.planning.draft.service.DraftService;
import com.travel.planning.retrieval.service.PlaceRetrievalService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RagDemoProfileTest {
    @ParameterizedTest
    @ValueSource(strings = {"test", "prod"})
    void ragControllersAreAbsentOutsideDevelopmentProfiles(String profile) {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withUserConfiguration(PlaceRetrievalController.class, PlaceIndexController.class, TravelIntentController.class, DraftController.class)
                .run(context -> assertThat(context)
                        .doesNotHaveBean(PlaceRetrievalController.class)
                        .doesNotHaveBean(PlaceIndexController.class)
                        .doesNotHaveBean(TravelIntentController.class)
                        .doesNotHaveBean(DraftController.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "rag-demo"})
    void ragControllersAreAvailableInBothDevelopmentProfiles(String profile) {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withUserConfiguration(Dependencies.class, PlaceRetrievalController.class, PlaceIndexController.class, TravelIntentController.class, DraftController.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(PlaceRetrievalController.class)
                        .hasSingleBean(PlaceIndexController.class)
                        .hasSingleBean(TravelIntentController.class)
                        .hasSingleBean(DraftController.class));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Dependencies {
        @Bean PlaceRetrievalService retrieval() { return mock(PlaceRetrievalService.class); }
        @Bean TravelIntentService intents() { return mock(TravelIntentService.class); }
        @Bean DraftService drafts() { return mock(DraftService.class); }
    }
}
