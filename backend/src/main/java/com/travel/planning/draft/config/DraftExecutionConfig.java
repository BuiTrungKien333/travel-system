package com.travel.planning.draft.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.*;

@Configuration
@Profile("rag-demo")
public class DraftExecutionConfig {

    @Bean(name = "ragDraftWeatherExecutor", destroyMethod = "shutdownNow")
    public ExecutorService weatherExecutor() {
        return new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16),
                Thread.ofPlatform().daemon(true).name("rag-weather-", 0).factory(), new ThreadPoolExecutor.AbortPolicy());
    }
}
