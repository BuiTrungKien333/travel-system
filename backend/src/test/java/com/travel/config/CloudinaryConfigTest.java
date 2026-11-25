package com.travel.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CloudinaryConfigTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(
        CloudinaryConfig.class
    );

    @Test
    void missingCredentialsDoNotPreventStartupOrCreateAFakeClient() {
        context.run(app -> assertThat(app).hasNotFailed().doesNotHaveBean(Cloudinary.class));
    }

    @Test
    void partialCredentialsDoNotEnableUploads() {
        context
            .withPropertyValues("app.cloudinary.cloud-name=fixture")
            .run(app -> assertThat(app).hasNotFailed().doesNotHaveBean(Cloudinary.class));
    }

    @Test
    void completeConfigurationCreatesTheClientWithoutCallingExternalServices() {
        context
            .withPropertyValues(
                "app.cloudinary.cloud-name=fixture",
                "app.cloudinary.api-key=fixture-key",
                "app.cloudinary.api-secret=fixture-secret"
            )
            .run(app -> assertThat(app).hasNotFailed().hasSingleBean(Cloudinary.class));
    }
}
