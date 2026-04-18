package io.droidevs.mclub.ai.retrieval.vector;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vector retrieval: embeds query and performs similarity search in pgvector.
 */
@Service
@RequiredArgsConstructor
public class VectorRetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorIndexRepository vectorIndexRepository;

    public List<VectorSearchResult> retrieveSimilar(ConversationContext ctx, String userMessage, int topK) {
        List<Double> emb = embeddingService.embed(userMessage);
        String literal = VectorSearchService.toPgvectorLiteral(emb);
        return vectorIndexRepository.search(literal, topK, null, null, null);
    }

    // Keep helper for any other callers
    static String toPgvectorLiteral(List<Double> emb) {
        return VectorSearchService.toPgvectorLiteral(emb);
    }
}
