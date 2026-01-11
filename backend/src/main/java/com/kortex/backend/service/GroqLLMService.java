package com.kortex.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

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
            @Value("${llm.groq.model:llama-3.3-70b-versatile}") String model,
            @Value("${llm.groq.max.tokens:1000}") int maxTokens,
            @Value("${llm.groq.temperature:0.7}") double temperature) {
        
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        // Fallback to OS env if property is missing or placeholder
        String resolvedKey = apiKey;
        if (resolvedKey == null || resolvedKey.isBlank() || "your_groq_api_key_here".equalsIgnoreCase(resolvedKey)) {
            String envKey = System.getenv("GROQ_API_KEY");
            if (envKey != null && !envKey.isBlank()) {
                resolvedKey = envKey;
                log.info("Groq API key loaded from environment variable.");
            } else {
                // Try to read from .env files if present
                String dotenvKey = tryReadKeyFromDotEnv();
                if (dotenvKey != null && !dotenvKey.isBlank()) {
                    resolvedKey = dotenvKey;
                    log.info("Groq API key loaded from .env file.");
                } else {
                    log.warn("Groq API key is not set. Configure llm.groq.api.key or environment variable GROQ_API_KEY.");
                }
            }
        }
        this.apiKey = resolvedKey;
        this.model = model;
        this.defaultMaxTokens = maxTokens;
        this.defaultTemperature = temperature;
        
        log.info("Groq LLM Service initialized with model: {}", model);
    }

    private String tryReadKeyFromDotEnv() {
        String[] candidates = new String[]{".env", "./backend/.env"};
        for (String candidate : candidates) {
            try {
                Path path = Paths.get(candidate);
                if (Files.exists(path)) {
                    try (Stream<String> lines = Files.lines(path)) {
                        String value = lines
                                .filter(line -> !line.trim().startsWith("#"))
                                .filter(line -> line.contains("GROQ_API_KEY"))
                                .map(line -> {
                                    int idx = line.indexOf('=');
                                    return idx > -1 ? line.substring(idx + 1).trim() : "";
                                })
                                .filter(s -> !s.isBlank())
                                .findFirst()
                                .orElse(null);
                        if (value != null && !value.isBlank()) {
                            return value;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Ignore errors reading .env
            }
        }
        return null;
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
