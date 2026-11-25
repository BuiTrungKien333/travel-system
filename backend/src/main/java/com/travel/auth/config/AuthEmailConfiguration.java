package com.travel.auth.config;

import java.time.Clock;
import java.util.concurrent.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthEmailProperties.class)
public class AuthEmailConfiguration {

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "authMailExecutor", destroyMethod = "shutdown")
    ExecutorService authMailExecutor() {
        return new ThreadPoolExecutor(
            2,
            2,
            0,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(100),
            Thread.ofPlatform().name("auth-mail-", 0).factory(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
