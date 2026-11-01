package com.travel.planning.retrieval;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.travel.planning.ai.intent.TravelIntentController;

import static org.assertj.core.api.Assertions.assertThat;

class RagDemoProfileTest {
    @Test
    void publicDemoControllersAreAbsentOutsideDemoProfile() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
                .withUserConfiguration(PlaceRetrievalController.class, PlaceIndexController.class, TravelIntentController.class)
                .run(context -> assertThat(context)
                        .doesNotHaveBean(PlaceRetrievalController.class)
                        .doesNotHaveBean(PlaceIndexController.class)
                        .doesNotHaveBean(TravelIntentController.class));
    }
}
