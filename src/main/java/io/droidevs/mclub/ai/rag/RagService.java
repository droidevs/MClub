package io.droidevs.mclub.ai.rag;

import io.droidevs.mclub.ai.agent.AgentLoopExecutor;
import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.conversation.ConversationSession;
import io.droidevs.mclub.ai.conversation.RagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Core orchestration entry.
 *
 * <p>Upgraded to delegate to {@link AgentLoopExecutor} for multi-step tool execution.
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final AgentLoopExecutor agentLoopExecutor;

    public RagResponse handle(ConversationSession session, ConversationContext ctx) {
        return agentLoopExecutor.run(session, ctx);
    }
}


