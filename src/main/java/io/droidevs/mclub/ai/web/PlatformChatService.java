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
    private final io.droidevs.mclub.repository.UserRepository userRepository;

    public RagResponse chat(String conversationId, String from, String text) {
        return chat(conversationId, from, text, null, null);
    }

    public RagResponse chat(String conversationId, String from, String text, java.util.UUID clubScopeId, java.util.UUID eventScopeId) {
        ConversationSession session = store.getOrCreate(conversationId, from);
        session = store.appendUserMessage(session, text);

        ConversationContext ctx = buildContext(from, clubScopeId, eventScopeId);
        RagResponse response = ragService.handle(session, ctx);

        store.appendAssistantMessage(session, response.message());
        return response;
    }

    private ConversationContext buildContext(String from, java.util.UUID clubScopeId, java.util.UUID eventScopeId) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return new ConversationContext(from, java.util.Optional.empty(), java.util.Optional.empty(), false,
                    java.util.Optional.ofNullable(clubScopeId), java.util.Optional.ofNullable(eventScopeId));
        }

        // In this project the principal username is the email (e.g. Jwt or form login).
        String email = auth.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return new ConversationContext(from, java.util.Optional.empty(), java.util.Optional.empty(), false,
                    java.util.Optional.ofNullable(clubScopeId), java.util.Optional.ofNullable(eventScopeId));
        }

        var userId = userRepository.findByEmail(email).map(io.droidevs.mclub.domain.User::getId);
        boolean linked = userId.isPresent();
        return new ConversationContext(from, userId, java.util.Optional.of(email), linked,
                java.util.Optional.ofNullable(clubScopeId), java.util.Optional.ofNullable(eventScopeId));
    }
}
