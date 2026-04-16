package io.droidevs.mclub.ai.rag;

/**
 * LLM gateway interface.
 *
 * <p>In production this can be backed by OpenAI/compatible APIs.
 * For now we keep a local deterministic implementation to enable end-to-end wiring.
 */
public interface LlmClient {
    LlmDecision decide(String prompt);
}

