package com.travel.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ConnectionCheckLogFilterTest {
    private final ConnectionCheckLogFilter filter = new ConnectionCheckLogFilter();

    @Test
    void reportsHealthOutcomeWithoutCredentialsQueryOrBody(CapturedOutput output) throws Exception {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader("Authorization", "Bearer test-only-do-not-log");
        request.addHeader("User-Agent", "okhttp/test-only-do-not-log");
        request.setQueryString("token=test-only-do-not-log");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> response.setStatus(503));
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(output.getAll()).contains("client=HTTP_CLIENT", "status=503", "completed=true")
                .doesNotContain("test-only-do-not-log", "Authorization", "token=");
    }

    @Test
    void doesNotLogOrBlockOtherEndpoints(CapturedOutput output) throws Exception {
        var called = new AtomicBoolean();
        filter.doFilter(new MockHttpServletRequest("POST", "/api/v1/auth/login"),
                new MockHttpServletResponse(), (req, res) -> called.set(true));
        assertThat(called).isTrue();
        assertThat(output.getAll()).doesNotContain("CONNECTION-CHECK");
    }
}
