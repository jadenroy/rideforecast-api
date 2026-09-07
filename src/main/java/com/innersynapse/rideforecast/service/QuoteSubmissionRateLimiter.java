package com.innersynapse.rideforecast.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class QuoteSubmissionRateLimiter {

    private final int limit;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    @Autowired
    public QuoteSubmissionRateLimiter(
            @Value("${rideforecast.submissions.max-per-minute:12}") int limit
    ) {
        this(limit, Clock.systemUTC());
    }

    QuoteSubmissionRateLimiter(int limit, Clock clock) {
        this.limit = Math.max(1, limit);
        this.clock = clock;
    }

    public Decision tryAcquire(String clientKey) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(1, ChronoUnit.MINUTES);
        if (requestCount.incrementAndGet() % 256 == 0) {
            removeExpiredClients(cutoff);
        }
        Deque<Instant> window = attempts.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());

        synchronized (window) {
            while (!window.isEmpty() && !window.getFirst().isAfter(cutoff)) {
                window.removeFirst();
            }

            if (window.size() >= limit) {
                int retryAfter = (int) Math.max(
                        1,
                        60 - ChronoUnit.SECONDS.between(window.getFirst(), now)
                );
                return new Decision(false, limit, 0, retryAfter);
            }

            window.addLast(now);
            return new Decision(true, limit, limit - window.size(), 0);
        }
    }

    private void removeExpiredClients(Instant cutoff) {
        attempts.forEach((key, window) -> {
            synchronized (window) {
                while (!window.isEmpty() && !window.getFirst().isAfter(cutoff)) {
                    window.removeFirst();
                }
                if (window.isEmpty()) {
                    attempts.remove(key, window);
                }
            }
        });
    }

    public record Decision(boolean allowed, int limit, int remaining, int retryAfterSeconds) {
    }
}
