package com.travel.security.jwt;

import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    @Mock
    private HandlerExceptionResolver resolver;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtAuthenticationEntryPoint entryPoint;

    @Test
    @DisplayName("commence should delegate the authentication exception to HandlerExceptionResolver")
    void commence_shouldDelegateToHandlerExceptionResolver() {
        AuthenticationException authException = new BadCredentialsException("Token is missing or invalid");

        entryPoint.commence(request, response, authException);

        verify(resolver).resolveException(request, response, null, authException);
    }
}
