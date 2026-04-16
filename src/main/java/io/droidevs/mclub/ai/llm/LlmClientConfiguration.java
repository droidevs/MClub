package io.droidevs.mclub.ai.llm;

import io.droidevs.mclub.ai.rag.LlmClient;
import io.droidevs.mclub.ai.rag.StubLlmClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensures there is always an LlmClient; OpenAiLlmClient overrides when enabled. */
@Configuration
public class LlmClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(LlmClient.class)
    LlmClient stubLlmClient() {
        return new StubLlmClient();
    }
}

