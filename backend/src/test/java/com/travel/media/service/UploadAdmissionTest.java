package com.travel.media.service;

import com.travel.common.exception.AppException;
import com.travel.media.config.MediaProperties;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class UploadAdmissionTest {
    @Test void boundsConcurrencyReleasesPermitsAndLimitsAttempts() {
        var limits = new MediaProperties(); limits.setUploadsPerMinute(2); limits.setConcurrentUploadsPerUser(1);
        var gate = new UploadAdmission(limits, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        var user = UUID.randomUUID();
        try (var permit = gate.acquire(user)) {
            assertThatThrownBy(() -> gate.acquire(user)).isInstanceOf(AppException.class);
        }
        gate.acquire(user).close();
        assertThatThrownBy(() -> gate.acquire(user)).isInstanceOf(AppException.class);
        gate.acquire(UUID.randomUUID()).close();
    }
}
