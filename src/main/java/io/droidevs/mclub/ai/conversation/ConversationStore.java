package io.droidevs.mclub.ai.conversation;

public interface ConversationStore {
    ConversationSession getOrCreate(String conversationId, String fromPhoneE164);

    ConversationSession appendUserMessage(ConversationSession session, String text);

    ConversationSession appendAssistantMessage(ConversationSession session, String text);
}

