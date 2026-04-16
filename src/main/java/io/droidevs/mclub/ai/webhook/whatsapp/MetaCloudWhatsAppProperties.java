package io.droidevs.mclub.ai.webhook.whatsapp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuration for Meta WhatsApp Cloud API sender. */
@Data
@Validated
@ConfigurationProperties(prefix = "mclub.whatsapp.meta")
public class MetaCloudWhatsAppProperties {

    /** Enable real sending via Meta Cloud API. */
    private boolean enabled = false;

    /** Graph API base URL, typically https://graph.facebook.com */
    @NotBlank
    private String baseUrl = "https://graph.facebook.com";

    /** Graph API version (e.g. v20.0). */
    @NotBlank
    private String apiVersion = "v20.0";

    /** Phone number id for the WhatsApp Business account (Meta Cloud). */
    @NotBlank
    private String phoneNumberId;

    /** Permanent access token / system user token. */
    @NotBlank
    private String accessToken;

    /** WhatsApp message template name for OTP linking. */
    private String otpTemplateName = "mclub_link_otp";

    /** Optional language code. */
    private String templateLanguage = "en_US";
}

