package io.droidevs.mclub.ai.link;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class OtpService {

    private final SecureRandom random = new SecureRandom();

    public String generateCode() {
        int code = 100_000 + random.nextInt(900_000);
        return String.valueOf(code);
    }

    public Instant expiresAt(Duration ttl) {
        return Instant.now().plus(ttl);
    }

    public String hash(String phoneE164, String code) {
        // Simple SHA-256 for demo; prefer HMAC with application secret.
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((phoneE164 + ":" + code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean matches(String expectedHash, String phoneE164, String code) {
        return expectedHash != null && expectedHash.equals(hash(phoneE164, code));
    }
}

