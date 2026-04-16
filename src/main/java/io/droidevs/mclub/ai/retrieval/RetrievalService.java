package io.droidevs.mclub.ai.retrieval;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.RetrievalContext;

/**
 * Retrieval layer used by RAG.
 *
 * <p>Implementation should be "read-only" and safe, using existing services/repositories.
 */
public interface RetrievalService {
    RetrievalContext retrieve(ConversationContext ctx, String userMessage);
}

