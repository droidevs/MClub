package io.droidevs.mclub.ai.ai;

import io.droidevs.mclub.ai.llm.OpenAiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class AiConfiguration {
}



