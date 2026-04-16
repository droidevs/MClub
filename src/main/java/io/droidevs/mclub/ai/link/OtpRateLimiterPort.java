package io.droidevs.mclub.ai.link;

/** Rate limiter abstraction for OTP start requests. */
public interface OtpRateLimiterPort {
    void requireAllowed(String phoneE164);
}

