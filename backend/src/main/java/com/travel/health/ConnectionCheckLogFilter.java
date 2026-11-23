package com.travel.health;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Dev-only diagnostics for the APK server check; never logs headers, query strings or bodies. */
@Component
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class ConnectionCheckLogFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equals(request.getMethod()) || !"/actuator/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String agent = request.getHeader("User-Agent");
        String client = agent != null && agent.contains("Mozilla/") ? "BROWSER" : "HTTP_CLIENT";
        long started = System.nanoTime();
        boolean completed = false;
        log.info("[CONNECTION-CHECK] received client={}", client);
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            log.info("[CONNECTION-CHECK] finished client={} status={} completed={} durationMs={}",
                    client, response.getStatus(), completed,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        }
    }
}
