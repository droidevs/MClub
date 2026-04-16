package io.droidevs.mclub.ai.conversation;

import java.time.Instant;
import java.util.List;

public record ConversationSession(
        String conversationId,
        String fromPhoneE164,
        List<ConversationMessage> messages,
        Instant createdAt
) {}

