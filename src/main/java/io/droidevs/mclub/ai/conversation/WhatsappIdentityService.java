package io.droidevs.mclub.ai.conversation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Maps WhatsApp phone numbers to MClub users.
 *
 * <p>Initial implementation is unlinked (read-only). In production, back this with a DB table
 * (phone <-> user) and a verification/linking flow.
 */
@Service
@RequiredArgsConstructor
public class WhatsappIdentityService {

    public ConversationContext buildContext(String fromPhoneE164) {
        // TODO: implement linking (e.g., UserWhatsAppLink table) and lookup.
        Optional<UUID> userId = Optional.empty();
        Optional<String> email = Optional.empty();
        return new ConversationContext(fromPhoneE164, userId, email, false);
    }
}

