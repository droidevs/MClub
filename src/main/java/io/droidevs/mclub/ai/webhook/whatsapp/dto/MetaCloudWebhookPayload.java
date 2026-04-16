package io.droidevs.mclub.ai.webhook.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Minimal Meta WhatsApp Cloud webhook payload (inbound). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaCloudWebhookPayload(
        List<Entry> entry
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(List<Change> changes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(Value value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            List<Message> messages,
            List<Contact> contacts
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String from, String id, String timestamp, Text text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Text(String body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(String wa_id) {}
}

