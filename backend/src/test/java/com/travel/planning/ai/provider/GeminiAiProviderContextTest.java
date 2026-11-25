package com.travel.planning.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.travel.planning.ai.config.AiConfigProperties;
import com.travel.planning.ai.copilot.context.AiCopilotContextBuilder;
import com.travel.planning.ai.copilot.fallback.FallbackAiCopilotGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class GeminiAiProviderContextTest {

    @Test
    @DisplayName(
        "Spring ApplicationContext should successfully instantiate GeminiAiProvider with @Autowired constructor"
    )
    void springContext_instantiatesGeminiAiProviderSuccessfully() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean(AiConfigProperties.class, AiConfigProperties::new);
            ctx.register(com.travel.config.AppConfig.class);
            ctx.registerBean(AiCopilotContextBuilder.class, AiCopilotContextBuilder::new);
            ctx.registerBean(FallbackAiCopilotGenerator.class, FallbackAiCopilotGenerator::new);
            ctx.register(GeminiAiProvider.class);
            ctx.register(AiProviderFactory.class);
            ctx.refresh();

            GeminiAiProvider provider = ctx.getBean(GeminiAiProvider.class);
            assertThat(provider).isNotNull();
            assertThat(provider.getProviderName()).isEqualTo("gemini");

            AiProviderFactory factory = ctx.getBean(AiProviderFactory.class);
            assertThat(factory).isNotNull();
        }
    }
}
