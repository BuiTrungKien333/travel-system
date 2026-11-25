package com.travel.security.config;

import com.travel.security.filter.JwtAuthenticationFilter;
import com.travel.security.jwt.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import com.travel.security.jwt.ApiAccessDeniedHandler;
import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final ApiAccessDeniedHandler accessDeniedHandler;
    private final Environment environment;
    private final com.travel.security.filter.ApiUsageLimitFilter apiUsageLimitFilter;

    private static final String[] WHITE_LIST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/google/challenge",
            "/api/v1/auth/google",
            "/api/v1/auth/google/verify-link",

    };

    @Value("${application.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint).accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, WHITE_LIST).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/health", "/actuator/health",
                                "/actuator/health/liveness", "/actuator/health/readiness", "/api/v1/destinations",
                                "/api/v1/public/places", "/api/v1/public/places/*",
                                "/api/v1/public/weather/current", "/api/v1/public/weather/forecast",
                                "/api/v1/auth/verify-email", "/api/v1/auth/google/verify-link").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .access(new WebExpressionAuthorizationManager(environment.acceptsProfiles(Profiles.of("dev", "test", "rag-demo"))
                                ? "permitAll" : "hasRole('ADMIN') and hasAuthority('admin:system:read')"))
                        .requestMatchers("/actuator/**").access(admin("admin:system:read"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/ai/executions").access(admin("admin:system:read"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/overview").access(admin("admin:system:read"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/integrations/health").access(admin("admin:system:read"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/integrations/health/check").access(admin("admin:system:read"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/integrations/health/check").access(admin("admin:system:read"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/knowledge").access(admin("admin:catalog"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/session").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/users/**").access(admin("admin:users"))
                        .requestMatchers("/api/v1/admin/catalog/**").access(admin("admin:catalog"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/planning/retrieval/index",
                                "/api/v1/admin/planning/retrieval/search",
                                "/api/v1/public/planning/retrieval/index").access(admin("admin:ai:index"))
                        .requestMatchers("/api/v1/admin/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").access(member("profile:update"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/home/dashboard").access(member("trip:read"))
                        .requestMatchers("/api/v1/trips/*/planning/drafts/**").access(member("planning:generate"))
                        .requestMatchers("/api/v1/trips/*/itinerary/**").access(member("itinerary:manage"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/trips", "/api/v1/trips/**").access(member("trip:read"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/trips").access(member("trip:create"))
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/trips/*").access(member("trip:delete"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/trips/**").access(member("trip:update"))
                        .requestMatchers(HttpMethod.PUT, "/api/v1/trips/**").access(member("trip:update"))
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/trips/**").access(member("trip:update"))
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/trips/*/places/*").access(member("trip:update"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/journal/memories/**").access(member("memory:read"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/journal/memories").access(member("memory:create"))
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/journal/memories/*").access(member("memory:update"))
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/journal/memories", "/api/v1/journal/memories/delete-memory").access(member("memory:delete"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/media/limits").access(member("media:upload"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/media/upload", "/api/v1/media/uploads").access(member("media:upload"))
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/media", "/api/v1/media/delete-media").access(member("media:delete"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/places/search", "/api/v1/places/addresses").access(member("maps:use"))
                        .requestMatchers(HttpMethod.POST, "/api/v1/routes").access(member("maps:use"))
                        .requestMatchers(HttpMethod.GET, "/api/v1/places/*").access(member("place:read"))
                        .requestMatchers("/api/v1/planning/**", "/api/v1/public/planning/**").access(member("planning:generate"))
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiUsageLimitFilter, org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private static WebExpressionAuthorizationManager member(String permission) {
        return new WebExpressionAuthorizationManager("hasAnyRole('USER','ADMIN') and hasAuthority('" + permission + "')");
    }

    private static WebExpressionAuthorizationManager admin(String permission) {
        return new WebExpressionAuthorizationManager("hasRole('ADMIN') and hasAuthority('" + permission + "')");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept-Language", "X-Request-ID"));
        config.setExposedHeaders(List.of("X-Request-ID", "X-Rate-Limit-Remaining"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
