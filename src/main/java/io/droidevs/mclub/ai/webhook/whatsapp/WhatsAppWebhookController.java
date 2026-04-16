package io.droidevs.mclub.ai.webhook.whatsapp;

import io.droidevs.mclub.ai.conversation.ConversationService;
import io.droidevs.mclub.ai.webhook.whatsapp.dto.WhatsAppWebhookRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WhatsApp inbound webhook adapter.
 *
 * <p>Provider-agnostic: map provider payload into {@link WhatsAppWebhookRequest}.
 * Outbound replies are delegated to {@link WhatsAppSender}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<Void> receive(@Valid @RequestBody WhatsAppWebhookRequest request) {
        conversationService.handleIncomingMessage(
                request.conversationId(),
                request.fromPhoneE164(),
                request.text()
        );
        // Webhooks should respond quickly; async processing is preferred.
        return ResponseEntity.ok().build();
    }
}

