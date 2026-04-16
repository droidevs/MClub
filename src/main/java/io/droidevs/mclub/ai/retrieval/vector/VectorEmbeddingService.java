package io.droidevs.mclub.ai.retrieval.vector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding generation service.
 *
 * <p>Production implementation should call the embedding endpoint of the chosen model provider.
 * This class is added as a stable seam without changing current system behavior.
 */
@Service
@RequiredArgsConstructor
public class VectorEmbeddingService {

    public List<Double> embed(String text) {
        throw new UnsupportedOperationException("Vector embeddings not implemented yet");
    }
}

