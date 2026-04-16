package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** Tool: list clubs (read-only). */
@Component
@RequiredArgsConstructor
public class ListClubsTool implements Tool {

    private final ClubService clubService;

    @Override
    public String name() {
        return "list_clubs";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        int pageSize = 10;
        try {
            Object limitRaw = call.arguments().get("limit");
            if (limitRaw != null) pageSize = Math.min(25, Math.max(1, Integer.parseInt(String.valueOf(limitRaw))));
        } catch (Exception ignored) {
        }

        var clubs = clubService.getAllClubs(PageRequest.of(0, pageSize)).getContent();
        if (clubs.isEmpty()) {
            return ToolResult.of("No clubs found.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Clubs:\n");
        for (var c : clubs) {
            sb.append("- ").append(c.getName()).append(" id=").append(c.getId()).append("\n");
        }
        return ToolResult.of(sb.toString().trim());
    }
}

