package io.droidevs.mclub.ai.link.web;

import io.droidevs.mclub.ai.link.WhatsAppLinkService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Web API to link WhatsApp number with the authenticated user.
 *
 * <p>For production you should deliver OTP via WhatsApp. This endpoint returns OTP for dev/testing.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/whatsapp/link")
public class WhatsAppLinkController {

    private final WhatsAppLinkService linkService;

    @PostMapping("/start")
    public StartResponse start(@RequestParam @NotBlank String phoneE164) {
        var r = linkService.startLink(phoneE164);
        // Do NOT return OTP to the caller in production.
        return new StartResponse(r.phoneE164(), r.expiresAt());
    }

    @PostMapping("/confirm")
    public void confirm(Authentication authentication,
                        @RequestParam @NotBlank String phoneE164,
                        @RequestParam @NotBlank String code) {
        linkService.confirmLink(authentication, phoneE164, code);
    }

    public record StartResponse(String phoneE164, Instant expiresAt) {}
}

