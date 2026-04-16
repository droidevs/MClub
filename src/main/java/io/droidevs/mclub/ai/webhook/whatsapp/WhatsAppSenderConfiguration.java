package io.droidevs.mclub.ai.webhook.whatsapp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures WhatsAppSender is always available.
 *
 * <p>When MetaCloudWhatsAppSender is enabled, it will be the WhatsAppSender bean.
 * Otherwise, fallback to LoggingWhatsAppSender.
 */
@Configuration
public class WhatsAppSenderConfiguration {

    @Bean
    @ConditionalOnMissingBean(WhatsAppSender.class)
    public WhatsAppSender loggingSender() {
        return new LoggingWhatsAppSender();
    }
}

