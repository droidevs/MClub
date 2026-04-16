package io.droidevs.mclub.ai.rag;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.conversation.ConversationMessage;
import io.droidevs.mclub.ai.conversation.ConversationSession;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/** Builder-pattern style prompt builder. */
@Component
public class PromptBuilder {

    public String buildPrompt(ConversationSession session, ConversationContext ctx, RetrievalContext retrieved) {
        int total = session.messages().size();
        int skip = Math.max(0, total - 10);
        String history = session.messages().stream()
                .skip(skip)
                .limit(10)
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));

        String snippets = String.join("\n---\n", retrieved.factualSnippets());

        return """
                You are MClub Assistant. Be concise and factual.

                Rules:
                - If you are not sure, ask a clarification question.
                - Do not claim to have performed an action unless a tool call was executed.
                - If user is not linked, do not perform actions. Explain linking.

                User linked: %s

                Retrieved context:
                %s

                Conversation:
                %s
                """.formatted(ctx.linked(), snippets, history);
    }
}


