package io.droidevs.mclub.ai.webhook.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Default sender for development: logs outgoing messages.
 * Replace with a Twilio/Meta implementation in production.
 */
@Slf4j
@Component
@Primary
public class LoggingWhatsAppSender implements WhatsAppSender {

    @Override
    public void sendText(String toPhoneE164, String message) {
        log.info("[WhatsApp->{}] {}", toPhoneE164, message);
    }
}

