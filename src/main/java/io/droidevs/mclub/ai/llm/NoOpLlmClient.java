package io.droidevs.mclub.ai.llm;

import io.droidevs.mclub.ai.rag.LlmClient;
import io.droidevs.mclub.ai.rag.LlmDecision;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Safe fallback LLM implementation.
 *
 * <p>This bean exists so the application context can start in environments where a real
 * LLM client isn't enabled (tests, local dev without OpenAI credentials).
 *
 * <p>When {@link OpenAiLlmClient} is enabled it will be used instead.
 */
@Component
@ConditionalOnProperty(prefix = "mclub.ai.openai", name = "enabled", havingValue = "false")
public class NoOpLlmClient implements LlmClient {

    @Override
    public LlmDecision decide(String prompt) {
        return LlmDecision.answer(
                "AI is not configured on this environment (no LLM client enabled). " +
                "Set mclub.ai.openai.enabled=true and configure OPENAI_API_KEY to enable the assistant."
        );
    }
}

