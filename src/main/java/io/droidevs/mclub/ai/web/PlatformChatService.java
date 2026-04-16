package io.droidevs.mclub.ai.web;

import io.droidevs.mclub.ai.conversation.*;
import io.droidevs.mclub.ai.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Synchronous chat entry point for in-platform web/mobile chat.
 *
 * <p>Does NOT send via WhatsAppSender. Returns response immediately.
 */
@Service
@RequiredArgsConstructor
public class PlatformChatService {

    private final ConversationStore store;
    private final RagService ragService;

    public RagResponse chat(String conversationId, String from, String text) {
        ConversationSession session = store.getOrCreate(conversationId, from);
        session = store.appendUserMessage(session, text);

        // For platform chat we don't have WhatsApp linking. Treat as anonymous/unlinked.
        ConversationContext ctx = new ConversationContext(from, java.util.Optional.empty(), java.util.Optional.empty(), false);
        RagResponse response = ragService.handle(session, ctx);

        store.appendAssistantMessage(session, response.message());
        return response;
    }
}

