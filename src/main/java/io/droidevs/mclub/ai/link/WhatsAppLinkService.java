package io.droidevs.mclub.ai.link;

import io.droidevs.mclub.ai.link.repository.UserWhatsAppLinkRepository;
import io.droidevs.mclub.ai.link.repository.WhatsAppLinkOtpRepository;
import io.droidevs.mclub.ai.webhook.whatsapp.MetaCloudWhatsAppSender;
import io.droidevs.mclub.ai.webhook.whatsapp.WhatsAppSender;
import io.droidevs.mclub.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WhatsAppLinkService {

    private final PhoneNormalizer phoneNormalizer;
    private final OtpService otpService;
    private final WhatsAppLinkOtpRepository otpRepo;
    private final UserWhatsAppLinkRepository linkRepo;
    private final CurrentUserService currentUserService;
    private final WhatsAppSender whatsAppSender;
    private final OtpRateLimiterPort otpRateLimiter;
    private final OtpAttemptPort otpAttemptService;

    /** Start linking: create OTP for the given phone number. */
    public StartLinkResponse startLink(String rawPhoneE164) {
        String phone = phoneNormalizer.normalizeE164(rawPhoneE164);
        otpRateLimiter.requireAllowed(phone);

        String code = otpService.generateCode();
        Instant expiresAt = otpService.expiresAt(Duration.ofMinutes(10));
        otpRepo.upsertChallenge(phone, otpService.hash(phone, code), expiresAt);

        // Deliver OTP via WhatsApp.
        // Production best practice: use an approved template (Meta requires templates for outbound outside the 24h window).
        if (whatsAppSender instanceof MetaCloudWhatsAppSender meta) {
            meta.sendOtpTemplate(phone, code);
        } else {
            // Dev/logging fallback.
            whatsAppSender.sendText(phone, "Your MClub linking code is: " + code + ". It expires in 10 minutes.");
        }

        return new StartLinkResponse(phone, expiresAt);
    }

    /** Confirm linking for the authenticated user. */
    public void confirmLink(Authentication auth, String rawPhoneE164, String code) {
        String phone = phoneNormalizer.normalizeE164(rawPhoneE164);
        otpAttemptService.requireNotLocked(phone);

        var challenge = otpRepo.findActiveByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("No active OTP challenge"));

        if (challenge.consumedAt() != null) {
            throw new IllegalArgumentException("OTP already consumed");
        }
        if (Instant.now().isAfter(challenge.expiresAt())) {
            throw new IllegalArgumentException("OTP expired");
        }
        if (!otpService.matches(challenge.codeHash(), phone, code)) {
            otpAttemptService.recordFailure(phone);
            throw new IllegalArgumentException("Invalid OTP");
        }

        UUID userId = currentUserService.requireUser(auth).getId();
        linkRepo.upsert(userId, phone, Instant.now());
        otpRepo.markConsumed(challenge.id());
        otpAttemptService.reset(phone);
    }

    public Optional<UUID> findUserIdByPhone(String rawPhoneE164) {
        try {
            String phone = phoneNormalizer.normalizeE164(rawPhoneE164);
            return linkRepo.findByPhone(phone).map(UserWhatsAppLinkRepository.LinkRow::userId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record StartLinkResponse(String phoneE164, Instant expiresAt) {}
}




