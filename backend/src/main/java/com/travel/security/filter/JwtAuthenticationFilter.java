package com.travel.security.filter;

import com.travel.security.jwt.JwtUtils;
import com.travel.security.jwt.TokenType;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "JWT_AUTHENTICATION_FILTER")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final WebAuthenticationDetailsSource DETAILS_SOURCE = new WebAuthenticationDetailsSource();

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String requestUri = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.trace("[JWT] No Bearer token found, skip authentication. URI={}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // TODO: implement token black list here (future)

        final Claims claims;
        try {
            claims = jwtUtils.extractAllClaims(jwt, TokenType.ACCESS_TOKEN);
            log.debug("[JWT] Token parsed successfully. Username={}, URI={}", claims.getSubject(), requestUri);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[JWT] Token parsing failed. URI={}, reason={}", requestUri, e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("[JWT] SecurityContext already authenticated, skip. URI={}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = claims.getSubject();
            String userIdStr = claims.get("userId", String.class);
            UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

            @SuppressWarnings("unchecked")
            List<String> authorityStrings = claims.get("authorities", List.class);

            Set<GrantedAuthority> authorities;
            Role role = null;

            if (authorityStrings != null) {
                authorities = new HashSet<>(authorityStrings.size());
                for (String auth : authorityStrings) {
                    if (auth == null || auth.isBlank())
                        continue;
                    authorities.add(new SimpleGrantedAuthority(auth));
                    if (role == null && auth.startsWith("ROLE_")) {
                        try {
                            role = Role.valueOf(auth.substring(5));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            } else {
                authorities = Collections.emptySet();
            }

            CustomUserDetails userDetails = new CustomUserDetails(userId, username, role, authorities);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

            authentication.setDetails(DETAILS_SOURCE.buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("[JWT] Authentication success. Username={}, URI={}", username, requestUri);
        } catch (RuntimeException e) {
            log.error("[JWT] Authentication process failed. URI={}", requestUri, e);
        }

        filterChain.doFilter(request, response);
    }
}
