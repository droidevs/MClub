package io.droidevs.mclub.ai.webhook.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Provider-agnostic representation of an inbound WhatsApp text message.
 *
 * <p>Adapt Twilio/Meta payloads into this DTO in a dedicated adapter if needed.
 */
public record WhatsAppWebhookRequest(
        @NotBlank String conversationId,
        @NotBlank String fromPhoneE164,
        @NotBlank String text
) {}

