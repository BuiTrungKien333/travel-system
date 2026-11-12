package com.travel.planning.service;

import com.travel.place.repository.PlaceRepository;
import com.travel.planning.ai.provider.AiProviderFactory;
import com.travel.planning.algorithm.HeldKarpSolver;
import com.travel.planning.rule.checker.TimelineCollisionRuleChecker;
import com.travel.planning.rule.service.RuleEvaluatorService;
import com.travel.planning.timeline.TimelineEngine;
import com.travel.weather.service.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class TripOptimizationServiceContextTest {

    @Test
    @DisplayName("Spring ApplicationContext instantiates TripOptimizationServiceImpl through constructor injection")
    void springContext_instantiatesTripOptimizationServiceImplSuccessfully() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(HeldKarpSolver.class, () -> Mockito.mock(HeldKarpSolver.class));
            ctx.registerBean(WeatherService.class, () -> Mockito.mock(WeatherService.class));
            ctx.registerBean(RuleEvaluatorService.class, () -> Mockito.mock(RuleEvaluatorService.class));
            ctx.registerBean(AiProviderFactory.class, () -> Mockito.mock(AiProviderFactory.class));
            ctx.registerBean(PlaceRepository.class, () -> Mockito.mock(PlaceRepository.class));
            ctx.register(TimelineEngine.class);
            ctx.register(TimelineCollisionRuleChecker.class);
            ctx.register(TripOptimizationServiceImpl.class);
            ctx.refresh();

            TripOptimizationService service = ctx.getBean(TripOptimizationService.class);
            assertThat(service).isNotNull();
            assertThat(service).isInstanceOf(TripOptimizationServiceImpl.class);
        }
    }
}
