package com.kortex.backend.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI implementation of LLM service.
 * Uses OpenAI's GPT models for text generation.
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
@Slf4j
public class OpenAILLMService implements LLMService {

    private final OpenAiService openAiService;
    private final String model;
    private final int defaultMaxTokens;
    private final double defaultTemperature;

    public OpenAILLMService(
            @Value("${llm.openai.api.key}") String apiKey,
            @Value("${llm.openai.model:gpt-3.5-turbo}") String model,
            @Value("${llm.openai.max.tokens:1000}") int maxTokens,
            @Value("${llm.openai.temperature:0.7}") double temperature) {
        
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
        this.model = model;
        this.defaultMaxTokens = maxTokens;
        this.defaultTemperature = temperature;
        
        log.info("OpenAI LLM Service initialized with model: {}", model);
    }

    @Override
    public String generateCompletion(String prompt) {
        return generateCompletion(prompt, defaultMaxTokens, defaultTemperature);
    }

    @Override
    public String generateCompletion(String prompt, int maxTokens, double temperature) {
        try {
            log.debug("Generating completion with OpenAI model: {}", model);
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(new ChatMessage(ChatMessageRole.USER.value(), prompt)))
                    .maxTokens(maxTokens)
                    .temperature(temperature)
                    .build();

            String completion = openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            log.debug("Generated completion: {} characters", completion.length());
            return completion;

        } catch (Exception e) {
            log.error("Error generating completion with OpenAI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate completion from OpenAI: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public String getModelName() {
        return model;
    }
}
