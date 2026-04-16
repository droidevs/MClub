package io.droidevs.mclub.ai.webhook.whatsapp;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbound WhatsApp sender using Meta WhatsApp Cloud API.
 *
 * <p>Enabled via: mclub.whatsapp.meta.enabled=true
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mclub.whatsapp.meta", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(MetaCloudWhatsAppProperties.class)
public class MetaCloudWhatsAppSender implements WhatsAppSender {

    private final MetaCloudWhatsAppProperties props;

    @Override
    public void sendText(String toPhoneE164, String text) {
        // Uses /{version}/{phone-number-id}/messages
        WebClient client = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String path = "/" + props.getApiVersion() + "/" + props.getPhoneNumberId() + "/messages";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", toPhoneE164);
        body.put("type", "text");
        body.put("text", Map.of(
                "preview_url", false,
                "body", text
        ));

        client.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(10))
                .block();
    }

    /**
     * Send OTP via a pre-approved WhatsApp template (recommended for production).
     *
     * <p>Template must be approved in WhatsApp Manager.
     */
    public void sendOtpTemplate(String toPhoneE164, String otpCode) {
        WebClient client = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String path = "/" + props.getApiVersion() + "/" + props.getPhoneNumberId() + "/messages";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", toPhoneE164);
        body.put("type", "template");
        body.put("template", Map.of(
                "name", props.getOtpTemplateName(),
                "language", Map.of("code", props.getTemplateLanguage()),
                "components", List.of(
                        Map.of(
                                "type", "body",
                                "parameters", List.of(
                                        Map.of("type", "text", "text", otpCode)
                                )
                        )
                )
        ));

        client.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(10))
                .block();
    }
}



