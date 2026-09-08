package com.innersynapse.rideforecast.auth;

import com.innersynapse.rideforecast.dto.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    public static final String IDENTITY_ATTRIBUTE = FirebaseAuthenticationFilter.class.getName() + ".identity";

    private final FirebaseTokenVerifier verifier;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public FirebaseAuthenticationFilter(
            FirebaseTokenVerifier verifier,
            ObjectMapper objectMapper,
            @Value("${rideforecast.auth.enabled:false}") boolean enabled
    ) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            unauthorized(response, "INVALID_AUTH_TOKEN", "Your sign-in expired or could not be verified.");
            return;
        }
        try {
            request.setAttribute(IDENTITY_ATTRIBUTE, verifier.verify(authorization.substring(7).trim()));
            filterChain.doFilter(request, response);
        } catch (InvalidAuthenticationException exception) {
            unauthorized(response, exception.getCode(), exception.getMessage());
        }
    }

    private void unauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(code, message));
    }
}
