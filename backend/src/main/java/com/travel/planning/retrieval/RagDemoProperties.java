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
    private String intentModel = "gemini-3.8-flash";
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
}
