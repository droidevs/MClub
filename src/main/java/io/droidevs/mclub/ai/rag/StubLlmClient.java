package io.droidevs.mclub.ai.rag;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic local "LLM" for development.
 *
 * <p>Recognizes very small command-like phrases to trigger tools.
 * Everything else returns a generic answer explaining available commands.
 */
public class StubLlmClient implements LlmClient {

    @Override
    public LlmDecision decide(String prompt) {
        // We don't parse the full prompt; we rely on the last user message being present.
        String lower = prompt.toLowerCase(Locale.ROOT);

        // Very small heuristics useful for wiring.
        if (lower.contains("tool:register_event")) {
            return LlmDecision.tool(new ToolCall("register_event", Map.of("eventQuery", "next")));
        }
        if (lower.contains("tool:checkin_event")) {
            return LlmDecision.tool(new ToolCall("checkin_event", Map.of("eventQuery", "current")));
        }
        if (lower.contains("tool:rate_event")) {
            Map<String, Object> args = new HashMap<>();
            args.put("eventQuery", "last");
            args.put("stars", 5);
            args.put("comment", "Great event");
            return LlmDecision.tool(new ToolCall("rate_event", args));
        }
        if (lower.contains("tool:comment")) {
            return LlmDecision.tool(new ToolCall("comment", Map.of(
                    "targetType", "EVENT",
                    "targetQuery", "last",
                    "text", "Nice"
            )));
        }

        return LlmDecision.answer(
                "I can help with MClub actions like event registration, check-in, rating, and comments. " +
                        "For now, try messages containing one of: tool:register_event, tool:checkin_event, tool:rate_event, tool:comment."
        );
    }
}

