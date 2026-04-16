package io.droidevs.mclub.ai.conversation;

import io.droidevs.mclub.ai.rag.RagService;
import io.droidevs.mclub.ai.webhook.whatsapp.WhatsAppSender;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Conversation/session layer.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Maintain lightweight conversation state (memory store can be swapped later)</li>
 *   <li>Enrich context (user identity mapping, locale)</li>
 *   <li>Delegate to {@link RagService}</li>
 *   <li>Send response back using {@link WhatsAppSender}</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationStore store;
    private final RagService ragService;
    private final WhatsAppSender whatsAppSender;
    private final WhatsappIdentityService identityService;

    /** Entry point from webhook controller. */
    @Async
    public void handleIncomingMessage(String conversationId, String fromPhoneE164, String text) {
        ConversationSession session = store.getOrCreate(conversationId, fromPhoneE164);
        session = store.appendUserMessage(session, text);

        ConversationContext ctx = identityService.buildContext(fromPhoneE164);
        RagResponse response = ragService.handle(session, ctx);

        store.appendAssistantMessage(session, response.message());
        whatsAppSender.sendText(fromPhoneE164, response.message());
    }
}

