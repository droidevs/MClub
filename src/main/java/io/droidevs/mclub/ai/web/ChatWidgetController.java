package io.droidevs.mclub.ai.web;

import io.droidevs.mclub.ai.conversation.ConversationService;
import io.droidevs.mclub.ai.web.dto.ChatMessageRequest;
import io.droidevs.mclub.ai.web.dto.ChatMessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Platform chatbot endpoint (non-WhatsApp) that reuses the same RAG pipeline.
 *
 * <p>Client: web widget / mobile app / admin UI.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatWidgetController {

    private final ConversationService conversationService;

    @PostMapping("/message")
    public ChatMessageResponse send(@RequestBody @Valid ChatMessageRequest req) {
        // For the platform widget, the channel is "web".
        // conversationId must be stable per user/session on the client.
        conversationService.handleIncomingMessage(req.conversationId(), req.from(), req.text());
        return new ChatMessageResponse("accepted");
    }
}

