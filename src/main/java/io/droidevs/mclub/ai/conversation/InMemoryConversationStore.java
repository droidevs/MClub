package io.droidevs.mclub.ai.conversation;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory store (dev only). Replace with Redis/DB for production. */
@Component
public class InMemoryConversationStore implements ConversationStore {

    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    @Override
    public ConversationSession getOrCreate(String conversationId, String fromPhoneE164) {
        return sessions.computeIfAbsent(conversationId, id -> new ConversationSession(
                id,
                fromPhoneE164,
                new ArrayList<>(),
                Instant.now(),
                null
        ));
    }

    @Override
    public ConversationSession appendUserMessage(ConversationSession session, String text) {
        session.messages().add(new ConversationMessage(ConversationMessage.Role.USER, text, Instant.now()));
        return session;
    }

    @Override
    public ConversationSession appendAssistantMessage(ConversationSession session, String text) {
        session.messages().add(new ConversationMessage(ConversationMessage.Role.ASSISTANT, text, Instant.now()));
        return session;
    }

    @Override
    public ConversationSession setPendingIntent(ConversationSession session, PendingIntent intent) {
        ConversationSession updated = new ConversationSession(
                session.conversationId(),
                session.fromPhoneE164(),
                session.messages(),
                session.createdAt(),
                intent
        );
        sessions.put(session.conversationId(), updated);
        return updated;
    }

    @Override
    public ConversationSession clearPendingIntent(ConversationSession session) {
        if (session.pendingIntent() == null) return session;
        ConversationSession updated = new ConversationSession(
                session.conversationId(),
                session.fromPhoneE164(),
                session.messages(),
                session.createdAt(),
                null
        );
        sessions.put(session.conversationId(), updated);
        return updated;
    }
}

