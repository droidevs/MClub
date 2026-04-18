package io.droidevs.mclub.ai.llm;

import org.springframework.context.annotation.Configuration;

/**
 * LLM client configuration.
 *
 * <p>OpenAI client is enabled via {@code mclub.ai.openai.enabled=true}.
 * This project is configured for OpenAI-only (no stub fallback).
 */
@Configuration
public class LlmClientConfiguration {
    // Intentionally empty.
}
