package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.dto.RegistrationDto;
import io.droidevs.mclub.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Tool: register the linked user to an event by ID. */
@Component
@RequiredArgsConstructor
public class RegisterToEventTool implements Tool {

    private final RegistrationService registrationService;

    @Override
    public String name() {
        return "register_event";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        String email = ctx.userEmail().orElse(null);
        if (email == null) {
            return ToolResult.of("To register for events, please link your account first (OTP linking). Ask me: 'link my account'.");
        }

        Object eventIdRaw = call.arguments().get("eventId");
        if (eventIdRaw == null) {
            return ToolResult.of("Please provide an eventId to register.");
        }

        UUID eventId = UUID.fromString(String.valueOf(eventIdRaw));
        RegistrationDto reg = registrationService.register(eventId, email);

        return ToolResult.of("Registered successfully for event " + reg.getEventId() + ".");
    }
}


