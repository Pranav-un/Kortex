package com.kortex.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Groq implementation of LLM service.
 * Uses Groq's ultra-fast LLM inference API.
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "groq")
@Slf4j
public class GroqLLMService implements LLMService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final int defaultMaxTokens;
    private final double defaultTemperature;

    public GroqLLMService(
            @Value("${llm.groq.api.url:https://api.groq.com/openai/v1}") String apiUrl,
            @Value("${llm.groq.api.key}") String apiKey,
            @Value("${llm.groq.model:llama3-8b-8192}") String model,
            @Value("${llm.groq.max.tokens:1000}") int maxTokens,
            @Value("${llm.groq.temperature:0.7}") double temperature) {
        
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.defaultMaxTokens = maxTokens;
        this.defaultTemperature = temperature;
        
        log.info("Groq LLM Service initialized with model: {}", model);
    }

    @Override
    public String generateCompletion(String prompt) {
        return generateCompletion(prompt, defaultMaxTokens, defaultTemperature);
    }

    @Override
    public String generateCompletion(String prompt, int maxTokens, double temperature) {
        try {
            log.debug("Generating completion with Groq model: {}", model);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "max_tokens", maxTokens,
                    "temperature", temperature
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = apiUrl + "/chat/completions";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String completion = (String) message.get("content");
                    
                    log.debug("Generated completion: {} characters", completion.length());
                    return completion;
                }
            }

            throw new RuntimeException("Invalid response from Groq API");

        } catch (Exception e) {
            log.error("Error generating completion with Groq: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate completion from Groq: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return "Groq";
    }

    @Override
    public String getModelName() {
        return model;
    }
}
