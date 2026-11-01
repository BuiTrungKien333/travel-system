package com.travel.planning.ai.resilience;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Profile("rag-demo")
public class AiRequestBudgetFilter extends OncePerRequestFilter {
    private final AiRequestContext context;
    public AiRequestBudgetFilter(AiRequestContext context) { this.context = context; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/public/planning/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try (var scope = context.ensure()) { chain.doFilter(request, response); }
    }
}
