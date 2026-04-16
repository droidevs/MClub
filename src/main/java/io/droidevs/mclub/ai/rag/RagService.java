package io.droidevs.mclub.ai.rag;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.conversation.ConversationSession;
import io.droidevs.mclub.ai.conversation.RagResponse;
import io.droidevs.mclub.ai.retrieval.RetrievalService;
import io.droidevs.mclub.ai.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Core orchestration: retrieval + LLM routing + tool execution.
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;

    public RagResponse handle(ConversationSession session, ConversationContext ctx) {
        String userMessage = session.messages().isEmpty() ? "" : session.messages().get(session.messages().size() - 1).content();

        RetrievalContext retrieved = retrievalService.retrieve(ctx, userMessage);
        String prompt = promptBuilder.buildPrompt(session, ctx, retrieved);

        LlmDecision decision = llmClient.decide(prompt);

        if (decision.toolCall().isPresent()) {
            if (!ctx.linked()) {
                return RagResponse.of("To perform actions (register, check-in, rate, comment), please link your WhatsApp number to your MClub account first.");
            }
            ToolCall call = decision.toolCall().get();
            var tool = toolRegistry.get(call.toolName());
            var result = tool.execute(call, ctx);
            return RagResponse.of(result.humanMessage());
        }

        return RagResponse.of(decision.answer());
    }
}


