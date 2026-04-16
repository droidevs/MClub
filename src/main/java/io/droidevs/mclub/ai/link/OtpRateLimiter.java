package io.droidevs.mclub.ai.link;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter for OTP start requests.
 *
 * <p>Production: move to Redis for multi-instance deployments.
 */
@Component
public class OtpRateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Allow N requests per window. */
    public void requireAllowed(String phoneE164) {
        int max = 5;
        Duration window = Duration.ofMinutes(15);

        Instant now = Instant.now();
        Window w = windows.compute(phoneE164, (ignored, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart.plus(window))) {
                return new Window(now, 1);
            }
            return new Window(existing.windowStart, existing.count + 1);
        });

        if (w.count > max) {
            throw new IllegalStateException("Too many OTP requests. Please try again later.");
        }
    }

    private record Window(Instant windowStart, int count) {}
}


