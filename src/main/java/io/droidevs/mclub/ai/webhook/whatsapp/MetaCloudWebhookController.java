package io.droidevs.mclub.ai.webhook.whatsapp;

import io.droidevs.mclub.ai.conversation.ConversationService;
import io.droidevs.mclub.ai.webhook.whatsapp.dto.MetaCloudWebhookPayload;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Meta WhatsApp Cloud webhook adapter.
 *
 * <p>Supports GET verification handshake and POST inbound messages.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks/whatsapp/meta")
public class MetaCloudWebhookController {

    private final ConversationService conversationService;
    private final MetaCloudProcessedMessageStore processedMessageStore;
    private final MetaCloudSignatureVerifier signatureVerifier;

    @Value("${mclub.whatsapp.meta.verify-token:}")
    private String verifyToken;

    /** Webhook verification handshake (Meta Cloud). */
    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String token,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && verifyToken != null && !verifyToken.isBlank() && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Forbidden");
    }

    /** Inbound messages. */
    @PostMapping
    public ResponseEntity<Void> receive(HttpServletRequest request, @RequestBody MetaCloudWebhookPayload payload) {
        // Verify webhook signature (do not process if invalid).
        if (!signatureVerifier.verify(request)) {
            return ResponseEntity.status(403).build();
        }

        if (payload == null || payload.entry() == null) {
            return ResponseEntity.ok().build();
        }

        payload.entry().forEach(entry -> {
            if (entry.changes() == null) return;
            entry.changes().forEach(change -> {
                var value = change.value();
                if (value == null || value.messages() == null) return;

                value.messages().forEach(msg -> {
                    if (msg == null) return;
                    String messageId = msg.id();
                    if (!processedMessageStore.markProcessedIfNew(messageId)) {
                        return; // duplicate delivery
                    }

                    String fromWa = msg.from(); // typically phone without '+'
                    String text = msg.text() != null ? msg.text().body() : "";

                    // Normalize to E.164-ish: add '+' prefix.
                    String fromPhoneE164 = fromWa == null ? "" : (fromWa.startsWith("+") ? fromWa : ("+" + fromWa));
                    String conversationId = "meta:" + fromPhoneE164;

                    conversationService.handleIncomingMessage(conversationId, fromPhoneE164, text);
                });
            });
        });

        return ResponseEntity.ok().build();
    }
}

