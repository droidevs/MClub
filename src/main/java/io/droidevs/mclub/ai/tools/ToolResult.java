package io.droidevs.mclub.ai.tools;

import java.util.Map;

/**
 * Result of executing a tool.
 *
 * @param humanMessage message safe to send to user
 * @param data optional structured payload (for logging / future channels)
 */
public record ToolResult(String humanMessage, Map<String, Object> data) {

    public static ToolResult of(String message) {
        return new ToolResult(message, Map.of());
    }

    public static ToolResult of(String message, Map<String, Object> data) {
        return new ToolResult(message, data == null ? Map.of() : data);
    }
}

