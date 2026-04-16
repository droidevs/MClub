package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.domain.Event;
import io.droidevs.mclub.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Tool: fetch event details by id (read-only). */
@Component
@RequiredArgsConstructor
public class GetEventDetailsTool implements Tool {

    private final EventService eventService;

    @Override
    public String name() {
        return "get_event_details";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        Object idRaw = call.arguments().get("eventId");
        if (idRaw == null) return ToolResult.of("Please provide eventId.");

        UUID id;
        try {
            id = UUID.fromString(String.valueOf(idRaw));
        } catch (Exception e) {
            return ToolResult.of("Invalid eventId. Expected UUID.");
        }

        Event e = eventService.getEvent(id);
        String msg = "Event details:\n" +
                "- title: " + safe(e.getTitle()) + "\n" +
                "- location: " + safe(e.getLocation()) + "\n" +
                "- start: " + e.getStartDate() + "\n" +
                "- end: " + e.getEndDate() + "\n" +
                "- clubId: " + (e.getClub() != null ? e.getClub().getId() : null);

        return ToolResult.of(msg);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}

