package com.travel.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.web.servlet.HandlerExceptionResolver;

class ApiUsageLimitFilterTest {

    @Test
    void publicBudgetIsBoundedAndCannotBeBypassedBySpoofingForwardedHeaders() throws Exception {
        var resolver = mock(HandlerExceptionResolver.class);
        var filter = new ApiUsageLimitFilter(
            Clock.fixed(Instant.parse("2026-11-17T00:00:10Z"), ZoneOffset.UTC),
            resolver
        );
        var chain = mock(FilterChain.class);
        for (int i = 0; i < 120; i++) filter.doFilter(
            new MockHttpServletRequest("GET", "/api/v1/public/weather/current"),
            new MockHttpServletResponse(),
            chain
        );
        var request = new MockHttpServletRequest("GET", "/api/v1/public/weather/current");
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain, times(120)).doFilter(any(), any());
        verify(resolver).resolveException(
            eq(request),
            eq(response),
            isNull(),
            argThat(e -> e instanceof AppException a && a.getErrorCode() == ErrorCode.REQUEST_RATE_LIMITED)
        );
        assertThat(response.getHeader("Retry-After")).isEqualTo("50");
    }

    @Test
    void unrelatedAndPersonalRequestsAreNotChargedAgainstThePublicBudget() throws Exception {
        var resolver = mock(HandlerExceptionResolver.class);
        var filter = new ApiUsageLimitFilter(Clock.systemUTC(), resolver);
        var chain = mock(FilterChain.class);
        for (int i = 0; i < 121; i++) filter.doFilter(
            new MockHttpServletRequest("GET", "/api/v1/trips"),
            new MockHttpServletResponse(),
            chain
        );
        verify(chain, times(121)).doFilter(any(), any());
        verifyNoInteractions(resolver);
    }
}
