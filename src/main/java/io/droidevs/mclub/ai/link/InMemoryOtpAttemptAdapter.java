package io.droidevs.mclub.ai.link;

import org.springframework.stereotype.Component;

/** Default in-memory adapter implementing the OTP attempt port. */
@Component
public class InMemoryOtpAttemptAdapter implements OtpAttemptPort {

    private final OtpAttemptService delegate;

    public InMemoryOtpAttemptAdapter(OtpAttemptService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void requireNotLocked(String phoneE164) {
        delegate.requireNotLocked(phoneE164);
    }

    @Override
    public void recordFailure(String phoneE164) {
        delegate.recordFailure(phoneE164);
    }

    @Override
    public void reset(String phoneE164) {
        delegate.reset(phoneE164);
    }
}
