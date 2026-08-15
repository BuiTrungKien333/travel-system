package com.travel.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/health/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Chưa sử dụng login form của Spring Security
                .formLogin(form -> form.disable())

                // Chưa sử dụng HTTP Basic
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}