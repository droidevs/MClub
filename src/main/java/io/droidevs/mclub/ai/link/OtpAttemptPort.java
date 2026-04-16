package io.droidevs.mclub.ai.link;

/** Abuse prevention abstraction for OTP confirm attempts. */
public interface OtpAttemptPort {
    void requireNotLocked(String phoneE164);
    void recordFailure(String phoneE164);
    void reset(String phoneE164);
}

