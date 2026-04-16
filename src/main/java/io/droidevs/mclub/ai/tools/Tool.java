package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;

/** Command/Strategy tool interface. */
public interface Tool {
    String name();

    ToolResult execute(ToolCall call, ConversationContext ctx);
}

