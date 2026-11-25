package com.travel.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.travel.security.jwt.JwtUtils;
import com.travel.security.jwt.TokenType;
import com.travel.security.model.CustomUserDetails;
import com.travel.user.enums.Role;
import com.travel.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository users;

    @Mock
    private com.travel.user.repository.RolePermissionRepository permissions;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should skip authentication when Authorization header is missing or does not start with Bearer")
    void doFilter_noBearerHeader_skipsAuth() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/v1/trips");
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("Should skip authentication when JWT token is invalid or expired")
    void doFilter_invalidToken_skipsAuth() throws ServletException, IOException {
        String invalidToken = "invalid_token_123";
        when(request.getRequestURI()).thenReturn("/api/v1/trips");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
        when(jwtUtils.extractAllClaims(invalidToken, TokenType.ACCESS_TOKEN)).thenThrow(
            new JwtException("Token expired")
        );

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should authenticate user and set CustomUserDetails as Principal when valid JWT is provided")
    void doFilter_validToken_authenticatesSuccessfully() throws ServletException, IOException {
        String validToken = "valid_token_xyz";
        UUID userId = UUID.randomUUID();
        String username = "user@travel.com";

        when(request.getRequestURI()).thenReturn("/api/v1/trips");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(jwtUtils.extractAllClaims(validToken, TokenType.ACCESS_TOKEN)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(username);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("sessionVersion", Long.class)).thenReturn(0L);
        UserRepository.AuthState user = mock(UserRepository.AuthState.class);
        when(user.getEmail()).thenReturn(username);
        when(user.getEmailVerifiedAt()).thenReturn(java.time.Instant.now());
        when(users.findAuthStateById(userId)).thenReturn(java.util.Optional.of(user));
        when(user.getRole()).thenReturn(Role.USER);
        when(permissions.findPermissionNamesByRole(Role.USER)).thenReturn(java.util.Set.of("trip:read", "trip:create"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();

        Object principal = auth.getPrincipal();
        assertThat(principal).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails userDetails = (CustomUserDetails) principal;

        assertThat(userDetails.getId()).isEqualTo(userId);
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getRole()).isEqualTo(Role.USER);

        List<String> authorityNames = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(authorityNames).containsExactlyInAnyOrder("ROLE_USER", "trip:read", "trip:create");

        verify(filterChain).doFilter(request, response);
    }
}
