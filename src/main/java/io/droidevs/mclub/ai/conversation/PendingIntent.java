package io.droidevs.mclub.ai.conversation;

import java.time.Instant;
import java.util.List;

/**
 * Stores a pending disambiguation/confirmation step.
 *
 * <p>Example: user says "register me for AI event" -> multiple matches -> assistant asks to pick 1/2/3.
 */
public record PendingIntent(
        String intentType,
        String originalUserMessage,
        List<Choice> choices,
        java.util.Map<String, Object> carryArgs,
        Instant createdAt
) {
    public record Choice(String label, String entityType, String entityId) {}

    public static PendingIntent of(String intentType,
                                  String originalUserMessage,
                                  List<Choice> choices,
                                  java.util.Map<String, Object> carryArgs,
                                  Instant createdAt) {
        return new PendingIntent(intentType,
                originalUserMessage,
                choices,
                carryArgs == null ? java.util.Map.of() : carryArgs,
                createdAt);
    }
}

