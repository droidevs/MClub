package io.droidevs.mclub.ai.link;

import org.springframework.stereotype.Component;

/** Basic E.164 normalizer. Assumes input is already close to E.164. */
@Component
public class PhoneNormalizer {

    public String normalizeE164(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // Remove spaces and dashes
        s = s.replaceAll("[\\s-]", "");
        if (!s.startsWith("+")) {
            // In production, use libphonenumber and require country.
            // Here we keep strict to avoid accidental wrong linking.
            throw new IllegalArgumentException("Phone number must be in E.164 format starting with +");
        }
        return s;
    }
}

