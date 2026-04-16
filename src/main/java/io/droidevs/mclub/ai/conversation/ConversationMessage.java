package io.droidevs.mclub.ai.conversation;

import java.time.Instant;

public record ConversationMessage(Role role, String content, Instant at) {
    public enum Role { USER, ASSISTANT }
}

