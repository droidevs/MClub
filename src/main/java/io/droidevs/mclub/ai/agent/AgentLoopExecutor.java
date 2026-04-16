package io.droidevs.mclub.ai.agent;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.conversation.ConversationMessage;
import io.droidevs.mclub.ai.conversation.ConversationSession;
import io.droidevs.mclub.ai.conversation.RagResponse;
import io.droidevs.mclub.ai.rag.LlmClient;
import io.droidevs.mclub.ai.rag.LlmDecision;
import io.droidevs.mclub.ai.rag.PromptBuilder;
import io.droidevs.mclub.ai.rag.RetrievalContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.ai.retrieval.RetrievalService;
import io.droidevs.mclub.ai.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Multi-step agent loop: retrieval -> LLM -> tool -> LLM ... until answer or maxSteps hit. */
@Component
@RequiredArgsConstructor
public class AgentLoopExecutor {

    @Qualifier("hybridRetrievalService")
    private final RetrievalService retrievalService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;

    /** Execute agent loop for the current session state. */
    public RagResponse run(ConversationSession session, ConversationContext ctx) {
        int maxSteps = 5;
        Set<String> loopGuard = new HashSet<>();

        ConversationSession localSession = session;

        for (int step = 0; step < maxSteps; step++) {
            String userMessage = localSession.messages().isEmpty()
                    ? ""
                    : localSession.messages().get(localSession.messages().size() - 1).content();

            RetrievalContext retrieved = retrievalService.retrieve(ctx, userMessage);
            String prompt = promptBuilder.buildPrompt(localSession, ctx, retrieved);

            LlmDecision decision = llmClient.decide(prompt);
            if (decision.toolCall().isEmpty()) {
                return RagResponse.of(decision.answer());
            }

            if (!ctx.linked()) {
                return RagResponse.of("To perform actions (register, check-in, rate, comment), please link your WhatsApp number to your MClub account first.");
            }

            ToolCall call = decision.toolCall().get();
            String signature = call.toolName() + ":" + call.arguments();
            if (!loopGuard.add(signature)) {
                return RagResponse.of("I seem to be looping on the same action. Can you clarify your request?");
            }

            var tool = toolRegistry.get(call.toolName());
            var result = tool.execute(call, ctx);

            // Feed tool result back as an ASSISTANT message (keeps existing message model unchanged).
            localSession = appendAssistant(localSession, "Tool[" + call.toolName() + "]: " + result.humanMessage());
        }

        return RagResponse.of("I couldn't complete the request within a safe number of steps. Please rephrase or be more specific.");
    }

    private ConversationSession appendAssistant(ConversationSession session, String text) {
        List<ConversationMessage> copy = new ArrayList<>(session.messages());
        copy.add(new ConversationMessage(ConversationMessage.Role.ASSISTANT, text, Instant.now()));
        return new ConversationSession(session.conversationId(), session.fromPhoneE164(), copy, session.createdAt());
    }
}




