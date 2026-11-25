package com.travel.planning.ai.provider;

import com.travel.planning.ai.config.AiConfigProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiProviderFactory {

    private final AiConfigProperties aiConfig;
    private final Map<String, AiProvider> providerMap;

    public AiProviderFactory(AiConfigProperties aiConfig, List<AiProvider> providers) {
        this.aiConfig = aiConfig;
        this.providerMap = providers
            .stream()
            .collect(Collectors.toMap(p -> p.getProviderName().toLowerCase(), Function.identity()));
        log.info("[AI_FACTORY] Registered AI providers: {}", providerMap.keySet());
    }

    /**
     * Lấy AI Provider được kích hoạt theo cấu hình application.ai.active-provider
     */
    public AiProvider getProvider() {
        String active =
            aiConfig.getActiveProvider() != null ? aiConfig.getActiveProvider().trim().toLowerCase() : "gemini";

        AiProvider provider = providerMap.get(active);
        if (provider == null) {
            log.warn("[AI_FACTORY] Configured AI provider '{}' not found. Falling back to 'mock'.", active);
            return getFallbackProvider();
        }

        if (!provider.isAvailable()) {
            log.warn(
                "[AI_FACTORY] AI provider '{}' is not available (e.g. missing API key). Falling back to 'mock'.",
                active
            );
            return getFallbackProvider();
        }

        return provider;
    }

    /**
     * Lấy AI Provider theo tên cụ thể (phục vụ testing hoặc lựa chọn runtime)
     */
    public Optional<AiProvider> getProvider(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(providerMap.get(name.trim().toLowerCase()));
    }

    private AiProvider getFallbackProvider() {
        AiProvider mock = providerMap.get("mock");
        if (mock != null) {
            return mock;
        }
        // Nếu không có mock, lấy bất kỳ provider nào đang có
        return providerMap
            .values()
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No AI Provider registered in application context"));
    }
}
