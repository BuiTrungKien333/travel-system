package com.travel.planning.retrieval;

import com.travel.planning.ai.intent.controller.TravelIntentController;
import com.travel.planning.draft.controller.DraftController;
import com.travel.planning.retrieval.controller.PlaceIndexController;
import com.travel.planning.retrieval.controller.PlaceRetrievalController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RagDemoProfileTest {
    @Test
    void publicDemoControllersAreAbsentOutsideDemoProfile() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
                .withUserConfiguration(PlaceRetrievalController.class, PlaceIndexController.class, TravelIntentController.class, DraftController.class)
                .run(context -> assertThat(context)
                        .doesNotHaveBean(PlaceRetrievalController.class)
                        .doesNotHaveBean(PlaceIndexController.class)
                        .doesNotHaveBean(TravelIntentController.class)
                        .doesNotHaveBean(DraftController.class));
    }
}
