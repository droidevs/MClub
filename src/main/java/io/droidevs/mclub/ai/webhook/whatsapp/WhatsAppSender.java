package io.droidevs.mclub.ai.webhook.whatsapp;

/** Adapter interface for sending WhatsApp messages (Twilio/Meta/etc). */
public interface WhatsAppSender {
    void sendText(String toPhoneE164, String message);
}

