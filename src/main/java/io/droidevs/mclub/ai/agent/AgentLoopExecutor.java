package io.droidevs.mclub.ai.agent;

import io.droidevs.mclub.ai.conversation.*;
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
    private final ConversationStore conversationStore;
    private final PendingIntentResolver pendingIntentResolver;

    /** Execute agent loop for the current session state. */
    public RagResponse run(ConversationSession session, ConversationContext ctx) {
        int maxSteps = 5;
        Set<String> loopGuard = new HashSet<>();

        ConversationSession localSession = session;

        for (int step = 0; step < maxSteps; step++) {
            String userMessage = lastUserMessage(localSession);

            RagResponse resolved = tryResolvePendingIntent(localSession, ctx, userMessage);
            if (resolved != null) {
                return resolved;
            }

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

            // Search-first enforcement for event-related actions
            ToolCall rewritten = enforceEventSearchFirst(localSession, ctx, userMessage, call);
            if (rewritten == null) {
                // enforceEventSearchFirst already returned a user response by storing pending intent
                return RagResponse.of("OK");
            }
            call = rewritten;

            var tool = toolRegistry.get(call.toolName());
            var result = tool.execute(call, ctx);

            // If user just asked to search, we can still offer a choice follow-up.
            RagResponse searchChoice = maybeStoreSearchChoiceAsPending(localSession, userMessage, call, result);
            if (searchChoice != null) {
                return searchChoice;
            }

            localSession = appendAssistant(localSession, "Tool[" + call.toolName() + "]: " + result.humanMessage());
        }

        return RagResponse.of("I couldn't complete the request within a safe number of steps. Please rephrase or be more specific.");
    }

    private String lastUserMessage(ConversationSession session) {
        if (session.messages().isEmpty()) return "";
        return session.messages().get(session.messages().size() - 1).content();
    }

    /** Returns a final user response if a pending intent is resolved; otherwise null to continue normal flow. */
    private RagResponse tryResolvePendingIntent(ConversationSession localSession, ConversationContext ctx, String userMessage) {
        if (localSession.pendingIntent() == null) return null;

        var res = pendingIntentResolver.resolve(userMessage, localSession.pendingIntent());
        if (res.type() == PendingIntentResolver.ResolutionType.CANCEL) {
            conversationStore.clearPendingIntent(localSession);
            return RagResponse.of("Ok, cancelled. What would you like to do instead?");
        }
        if (res.type() != PendingIntentResolver.ResolutionType.CHOICE) {
            return null;
        }

        PendingIntent pending = localSession.pendingIntent();
        PendingIntent.Choice choice = pending.choices().get(res.choiceIndexZeroBased());
        conversationStore.clearPendingIntent(localSession);

        ToolCall nextCall = buildToolCallFromChoice(pending, choice);
        if (nextCall == null) {
            return RagResponse.of("Got it. I need a bit more detail on what to do with that choice. Please restate your request.");
        }

        var tool = toolRegistry.get(nextCall.toolName());
        var result = tool.execute(nextCall, ctx);
        appendAssistant(localSession, "Tool[" + nextCall.toolName() + "]: " + result.humanMessage());
        return RagResponse.of(result.humanMessage());
    }

    private ToolCall buildToolCallFromChoice(PendingIntent pending, PendingIntent.Choice choice) {
        java.util.Map<String, Object> carry = pending.carryArgs() == null ? java.util.Map.of() : pending.carryArgs();

        if ("REGISTER_EVENT".equalsIgnoreCase(pending.intentType()) && "EVENT".equalsIgnoreCase(choice.entityType())) {
            return new ToolCall("register_event", java.util.Map.of("eventId", choice.entityId()));
        }
        if ("RATE_EVENT".equalsIgnoreCase(pending.intentType()) && "EVENT".equalsIgnoreCase(choice.entityType())) {
            var args = new java.util.HashMap<String, Object>(carry);
            args.put("eventId", choice.entityId());
            return new ToolCall("rate_event", args);
        }
        if ("ADD_COMMENT_EVENT".equalsIgnoreCase(pending.intentType()) && "EVENT".equalsIgnoreCase(choice.entityType())) {
            var args = new java.util.HashMap<String, Object>(carry);
            args.put("targetType", "EVENT");
            args.put("targetId", choice.entityId());
            return new ToolCall("add_comment", args);
        }
        if (("GET_EVENT_DETAILS".equalsIgnoreCase(pending.intentType()) || "SEARCH_EVENTS_CHOICE".equalsIgnoreCase(pending.intentType()))
                && "EVENT".equalsIgnoreCase(choice.entityType())) {
            return new ToolCall("get_event_details", java.util.Map.of("eventId", choice.entityId()));
        }
        return null;
    }

    private ToolCall enforceEventSearchFirst(ConversationSession localSession, ConversationContext ctx, String userMessage, ToolCall call) {
        // Only enforce for tools that need eventId/targetId resolution.
        switch (call.toolName()) {
            case "get_event_details" -> {
                if (call.arguments().get("eventId") != null) return call;
                return resolveEventBySearchOrAsk(localSession, ctx, userMessage, call, "eventQuery", "GET_EVENT_DETAILS", java.util.Map.of());
            }
            case "register_event" -> {
                if (call.arguments().get("eventId") != null) return call;
                return resolveEventBySearchOrAsk(localSession, ctx, userMessage, call, "eventQuery", "REGISTER_EVENT", java.util.Map.of());
            }
            case "rate_event" -> {
                if (call.arguments().get("eventId") != null) return call;
                var carryArgs = new java.util.HashMap<String, Object>(call.arguments());
                carryArgs.remove("eventId");
                carryArgs.remove("eventQuery");
                return resolveEventBySearchOrAsk(localSession, ctx, userMessage, call, "eventQuery", "RATE_EVENT", carryArgs);
            }
            case "add_comment" -> {
                if (!"EVENT".equalsIgnoreCase(String.valueOf(call.arguments().get("targetType")))) return call;
                if (call.arguments().get("targetId") != null) return call;
                var carryArgs = new java.util.HashMap<String, Object>(call.arguments());
                carryArgs.remove("targetId");
                carryArgs.remove("targetQuery");
                // We reuse event search to resolve targetId.
                return resolveEventBySearchOrAsk(localSession, ctx, userMessage, call, "targetQuery", "ADD_COMMENT_EVENT", carryArgs);
            }
            default -> {
                return call;
            }
        }
    }

    /**
     * If exactly one event is found -> returns a rewritten ToolCall with ID filled.
     * If multiple -> stores PendingIntent + returns null (caller should return a user reply).
     */
    private ToolCall resolveEventBySearchOrAsk(ConversationSession localSession,
                                              ConversationContext ctx,
                                              String userMessage,
                                              ToolCall originalCall,
                                              String queryArgKey,
                                              String pendingIntentType,
                                              java.util.Map<String, Object> carryArgs) {
        Object q = originalCall.arguments().get(queryArgKey);
        if (q == null || String.valueOf(q).isBlank()) q = userMessage;

        var searchTool = toolRegistry.get("search_events");
        var searchResult = searchTool.execute(new ToolCall("search_events", java.util.Map.of(
                "query", String.valueOf(q),
                "limit", 5
        )), ctx);

        List<?> candidates = extractCandidates(searchResult);
        if (candidates == null || candidates.isEmpty()) {
            return null; // will end up as a clarification response by caller
        }

        if (candidates.size() == 1 && candidates.get(0) instanceof java.util.Map<?, ?> m) {
            Object id = m.get("entityId");
            if (id == null) return null;
            String idStr = String.valueOf(id);

            // Rewrite the call with the resolved id.
            var args = new java.util.HashMap<String, Object>(originalCall.arguments());
            if ("add_comment".equals(originalCall.toolName())) {
                args.put("targetId", idStr);
            } else {
                args.put("eventId", idStr);
            }
            return new ToolCall(originalCall.toolName(), args);
        }

        // multiple results -> set pending intent
        List<PendingIntent.Choice> choices = toChoices(candidates);
        conversationStore.setPendingIntent(localSession,
                PendingIntent.of(pendingIntentType, userMessage, choices, carryArgs, Instant.now()));
        return null;
    }

    private RagResponse maybeStoreSearchChoiceAsPending(ConversationSession localSession, String userMessage, ToolCall call, io.droidevs.mclub.ai.tools.ToolResult result) {
        if (!"search_events".equals(call.toolName())) return null;
        List<?> candidates = extractCandidates(result);
        if (candidates == null || candidates.size() <= 1) return null;

        List<PendingIntent.Choice> choices = toChoices(candidates);
        conversationStore.setPendingIntent(localSession,
                PendingIntent.of("SEARCH_EVENTS_CHOICE", userMessage, choices, java.util.Map.of(), Instant.now()));
        return RagResponse.of(result.humanMessage() + "\n\nReply with a number (1-" + choices.size() + ") to choose, or 'cancel'.");
    }

    private List<?> extractCandidates(io.droidevs.mclub.ai.tools.ToolResult toolResult) {
        Object raw = toolResult.data().get("candidates");
        if (raw instanceof List<?> l) return l;
        return null;
    }

    private List<PendingIntent.Choice> toChoices(List<?> candidates) {
        List<PendingIntent.Choice> choices = new ArrayList<>();
        for (Object o : candidates) {
            if (o instanceof java.util.Map<?, ?> m) {
                Object id = m.get("entityId");
                Object type = m.get("entityType");
                Object label = m.get("snippet");
                if (id != null && type != null) {
                    choices.add(new PendingIntent.Choice(
                            label == null ? String.valueOf(id) : String.valueOf(label),
                            String.valueOf(type),
                            String.valueOf(id)
                    ));
                }
            }
        }
        return choices;
    }

    private ConversationSession appendAssistant(ConversationSession session, String text) {
        List<ConversationMessage> copy = new ArrayList<>(session.messages());
        copy.add(new ConversationMessage(ConversationMessage.Role.ASSISTANT, text, Instant.now()));
        return new ConversationSession(session.conversationId(), session.fromPhoneE164(), copy, session.createdAt(), session.pendingIntent());
    }
}

