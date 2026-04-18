package io.droidevs.mclub.ai.link;

import org.springframework.stereotype.Component;

/** Default in-memory adapter implementing the rate limiter port. */
@Component
public class InMemoryOtpRateLimiterAdapter implements OtpRateLimiterPort {

    private final OtpRateLimiter delegate;

    public InMemoryOtpRateLimiterAdapter(OtpRateLimiter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void requireAllowed(String phoneE164) {
        delegate.requireAllowed(phoneE164);
    }
}
