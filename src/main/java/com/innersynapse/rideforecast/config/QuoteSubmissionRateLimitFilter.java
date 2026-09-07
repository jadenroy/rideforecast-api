package com.innersynapse.rideforecast.config;

import com.innersynapse.rideforecast.service.QuoteSubmissionRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class QuoteSubmissionRateLimitFilter extends OncePerRequestFilter {

    private final QuoteSubmissionRateLimiter limiter;

    public QuoteSubmissionRateLimitFilter(QuoteSubmissionRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/v1/quotes".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        QuoteSubmissionRateLimiter.Decision decision = limiter.tryAcquire(clientKey(request));
        response.setHeader("X-RateLimit-Limit", Integer.toString(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(decision.remaining()));

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", Integer.toString(decision.retryAfterSeconds()));
        response.getWriter().write("""
                {"code":"RATE_LIMITED","message":"Too many quote submissions. Your trip is still saved; wait a moment and try again.","fieldErrors":{},"retryAfterSeconds":%d}
                """.formatted(decision.retryAfterSeconds()).trim());
    }

    private String clientKey(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        return address == null || address.isBlank() ? "unknown" : address;
    }
}
