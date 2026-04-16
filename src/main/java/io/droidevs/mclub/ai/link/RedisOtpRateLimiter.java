package io.droidevs.mclub.ai.link;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Redis-backed OTP start rate limiting (multi-instance safe). */
@Component
@ConditionalOnProperty(prefix = "mclub.whatsapp.linking", name = "rate-limit.redis-enabled", havingValue = "true")
public class RedisOtpRateLimiter implements OtpRateLimiterPort {

    private final StringRedisTemplate redis;

    public RedisOtpRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void requireAllowed(String phoneE164) {
        int max = 5;
        Duration window = Duration.ofMinutes(15);

        String key = "otp:start:" + phoneE164;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }

        if (count != null && count > max) {
            throw new IllegalStateException("Too many OTP requests. Please try again later.");
        }
    }
}

