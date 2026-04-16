package io.droidevs.mclub.ai.link;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks OTP verification attempts per phone to slow brute force guessing.
 *
 * <p>Production: move to Redis + add IP-based controls.
 */
@Component
public class OtpAttemptService {

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public void recordFailure(String phoneE164) {
        Duration window = Duration.ofMinutes(10);
        Instant now = Instant.now();

        attempts.compute(phoneE164, (ignored, existing) -> {
            if (existing == null || now.isAfter(existing.windowStart.plus(window))) {
                return new Attempts(now, 1);
            }
            return new Attempts(existing.windowStart, existing.failures + 1);
        });
    }

    public void reset(String phoneE164) {
        attempts.remove(phoneE164);
    }

    public void requireNotLocked(String phoneE164) {
        int maxFailures = 8;
        Attempts a = attempts.get(phoneE164);
        if (a != null && a.failures >= maxFailures) {
            throw new IllegalStateException("Too many invalid attempts. Please request a new code.");
        }
    }

    private record Attempts(Instant windowStart, int failures) {}
}


