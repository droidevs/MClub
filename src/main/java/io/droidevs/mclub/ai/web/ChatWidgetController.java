package io.droidevs.mclub.ai.web;

import io.droidevs.mclub.ai.conversation.ConversationMessage;
import io.droidevs.mclub.ai.conversation.ConversationSession;
import io.droidevs.mclub.ai.conversation.ConversationStore;
import io.droidevs.mclub.ai.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Platform chatbot endpoint (non-WhatsApp) that reuses the same RAG pipeline.
 *
 * <p>Client: web widget / mobile app / admin UI.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatWidgetController {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    private final PlatformChatService platformChatService;
    private final ConversationStore conversationStore;

    @PostMapping("/message")
    public ChatMessageResponse send(jakarta.servlet.http.HttpSession httpSession,
                                    @RequestBody @Valid ChatMessageRequest req) {
        String conversationId = (req.conversationId() == null || req.conversationId().isBlank())
                ? "web:" + httpSession.getId()
                : req.conversationId();

        var r = platformChatService.chat(conversationId, req.from(), req.text());
        return new ChatMessageResponse(r.message());
    }

    @GetMapping("/history")
    public ChatHistoryResponse history(jakarta.servlet.http.HttpSession httpSession,
                                       @RequestParam(value = "conversationId", required = false) String conversationIdParam) {
        String conversationId = (conversationIdParam == null || conversationIdParam.isBlank())
                ? "web:" + httpSession.getId()
                : conversationIdParam;

        ConversationSession session = conversationStore.getOrCreate(conversationId, "web");

        List<ChatHistoryResponse.ChatHistoryMessage> messages = session.messages().stream()
                .map(m -> new ChatHistoryResponse.ChatHistoryMessage(
                        m.role() == ConversationMessage.Role.USER ? "user" : "ai",
                        m.content(),
                        ISO_INSTANT.format(m.at().atOffset(ZoneOffset.UTC))
                ))
                .toList();

        return new ChatHistoryResponse(messages);
    }

    @DeleteMapping("/history")
    public ClearChatResponse clearHistory(jakarta.servlet.http.HttpSession httpSession,
                                          @RequestParam(value = "conversationId", required = false) String conversationIdParam) {
        String conversationId = (conversationIdParam == null || conversationIdParam.isBlank())
                ? "web:" + httpSession.getId()
                : conversationIdParam;

        ConversationSession session = conversationStore.getOrCreate(conversationId, "web");
        session.messages().clear();
        return new ClearChatResponse(true);
    }

    @GetMapping("/conversation-id")
    public ConversationIdResponse conversationId(jakarta.servlet.http.HttpSession httpSession) {
        return new ConversationIdResponse("web:" + httpSession.getId());
    }
}
