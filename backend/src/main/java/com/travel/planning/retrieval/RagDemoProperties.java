package com.travel.planning.retrieval;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("rag-demo")
@ConfigurationProperties("application.rag")
@Getter
@Setter
public class RagDemoProperties {
    private String apiKey = "";
    private String embeddingModel = "gemini-embedding-001";
    private String intentModel = "gemini-3.5-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private long requestDeadlineMillis = 60_000;
    private long callTimeoutMillis = 30_000;
    private long retryBackoffMillis = 500;
}
