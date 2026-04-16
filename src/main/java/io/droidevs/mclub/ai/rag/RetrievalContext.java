package io.droidevs.mclub.ai.rag;

import java.util.List;

/** Context returned by retrieval layer to inject into prompts. */
public record RetrievalContext(
        List<String> factualSnippets,
        List<String> recentEvents
) {
    public static RetrievalContext empty() {
        return new RetrievalContext(List.of(), List.of());
    }
}

