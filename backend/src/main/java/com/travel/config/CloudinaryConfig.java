package com.travel.config;

import com.cloudinary.Cloudinary;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

@Configuration
public class CloudinaryConfig {

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    @Conditional(Configured.class)
    public Cloudinary cloudinary() {
        return new Cloudinary(
            Map.of("cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret, "secure", true)
        );
    }

    static class Configured implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            var environment = context.getEnvironment();
            return (
                StringUtils.hasText(environment.getProperty("app.cloudinary.cloud-name")) &&
                StringUtils.hasText(environment.getProperty("app.cloudinary.api-key")) &&
                StringUtils.hasText(environment.getProperty("app.cloudinary.api-secret"))
            );
        }
    }
}
