package io.droidevs.mclub.ai.webhook.whatsapp;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Dev-only in-memory idempotency store. Replace with Redis for production. */
@Component
public class InMemoryMetaCloudProcessedMessageStore implements MetaCloudProcessedMessageStore {

    private final Map<String, Instant> processed = new ConcurrentHashMap<>();
    private final Duration ttl = Duration.ofHours(24);

    @Override
    public boolean markProcessedIfNew(String messageId) {
        if (messageId == null || messageId.isBlank()) return true; // no id, process
        cleanup();
        return processed.putIfAbsent(messageId, Instant.now()) == null;
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minus(ttl);
        processed.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
    }
}

