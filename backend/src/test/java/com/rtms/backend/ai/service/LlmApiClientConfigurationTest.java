package com.rtms.backend.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmApiClientConfigurationTest {

    @Test
    void bindsAiApiPropertiesFromSpringContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "ai-api-test",
                    Map.of(
                            "ai.api.base-url", "https://ai.example.test/v1",
                            "ai.api.key", "test-api-key",
                            "ai.api.model", "test-model"
                    )
            ));
            context.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper());
            context.register(LlmApiClient.class);
            context.refresh();

            LlmApiClient client = context.getBean(LlmApiClient.class);

            assertEquals("https://ai.example.test/v1", ReflectionTestUtils.getField(client, "aiBaseUrl"));
            assertEquals("test-api-key", ReflectionTestUtils.getField(client, "aiApiKey"));
            assertEquals("test-model", client.getAiModel());
            assertTrue(client.isApiKeyConfigured());
        }
    }
}
