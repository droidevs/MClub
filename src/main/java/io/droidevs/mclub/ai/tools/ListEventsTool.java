package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** Tool: list events (read-only). */
@Component
@RequiredArgsConstructor
public class ListEventsTool implements Tool {

    private final EventService eventService;

    @Override
    public String name() {
        return "list_events";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        int pageSize = 10;
        try {
            Object limitRaw = call.arguments().get("limit");
            if (limitRaw != null) pageSize = Math.min(25, Math.max(1, Integer.parseInt(String.valueOf(limitRaw))));
        } catch (Exception ignored) {
        }

        var events = eventService.getAllEvents(PageRequest.of(0, pageSize)).getContent();
        if (events.isEmpty()) {
            return ToolResult.of("No events found.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Events:\n");
        for (var e : events) {
            sb.append("- ").append(e.getTitle())
                    .append(" (start=").append(e.getStartDate()).append(")")
                    .append(" id=").append(e.getId())
                    .append("\n");
        }
        return ToolResult.of(sb.toString().trim());
    }
}

