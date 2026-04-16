package io.droidevs.mclub.ai.rag;

import java.util.Map;

/**
 * A structured tool invocation decided by the LLM.
 *
 * @param toolName registry key
 * @param arguments tool-specific arguments (already extracted)
 */
public record ToolCall(String toolName, Map<String, Object> arguments) {
}

