package io.droidevs.mclub.ai.rag;

import java.util.Optional;

/**
 * Output of the LLM routing step.
 *
 * <p>Either:
 * <ul>
 *   <li>a direct answer (no tool call), or</li>
 *   <li>a tool call with structured arguments.</li>
 * </ul>
 */
public record LlmDecision(String answer, Optional<ToolCall> toolCall) {

    public static LlmDecision answer(String text) {
        return new LlmDecision(text, Optional.empty());
    }

    public static LlmDecision tool(ToolCall call) {
        return new LlmDecision("", Optional.of(call));
    }
}

