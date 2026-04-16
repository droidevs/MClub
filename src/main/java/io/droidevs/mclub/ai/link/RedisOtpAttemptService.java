package io.droidevs.mclub.ai.link;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Redis-backed OTP attempt tracker (multi-instance safe). */
@Component
@ConditionalOnProperty(prefix = "mclub.whatsapp.linking", name = "attempts.redis-enabled", havingValue = "true")
public class RedisOtpAttemptService implements OtpAttemptPort {

    private final StringRedisTemplate redis;

    public RedisOtpAttemptService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void requireNotLocked(String phoneE164) {
        int maxFailures = 8;
        String key = "otp:fail:" + phoneE164;
        String raw = redis.opsForValue().get(key);
        if (raw == null) return;
        try {
            long failures = Long.parseLong(raw);
            if (failures >= maxFailures) {
                throw new IllegalStateException("Too many invalid attempts. Please request a new code.");
            }
        } catch (NumberFormatException ignored) {
            // reset corrupted value
            redis.delete(key);
        }
    }

    @Override
    public void recordFailure(String phoneE164) {
        Duration window = Duration.ofMinutes(10);
        String key = "otp:fail:" + phoneE164;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }
    }

    @Override
    public void reset(String phoneE164) {
        redis.delete("otp:fail:" + phoneE164);
    }
}

