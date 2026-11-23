package com.travel.telemetry;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class TelemetryConfig {
    @Bean(destroyMethod="close")
    @Profile("!test")
    @ConditionalOnProperty(name="telemetry.enabled",havingValue="true")
    MongoClient telemetryMongoClient(@Value("${telemetry.uri}") String uri) {
        return MongoClients.create(MongoClientSettings.builder().applyConnectionString(new ConnectionString(uri))
            .applyToClusterSettings(s->s.serverSelectionTimeout(1500,TimeUnit.MILLISECONDS))
            .applyToSocketSettings(s->s.connectTimeout(1000,TimeUnit.MILLISECONDS).readTimeout(2000,TimeUnit.MILLISECONDS))
            .applyToConnectionPoolSettings(s->s.maxSize(5))
            .writeConcern(com.mongodb.WriteConcern.MAJORITY.withJournal(true)).retryWrites(true).build());
    }
}
