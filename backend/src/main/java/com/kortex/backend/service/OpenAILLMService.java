package com.kortex.backend.service;

/**
 * OpenAI service is disabled. Project standardized on Groq.
 * This stub avoids optional dependency conflicts with Jackson 3.
 */
@Deprecated
public class OpenAILLMService implements LLMService {

    public OpenAILLMService() {}

    @Override
    public String generateCompletion(String prompt) {
        throw new UnsupportedOperationException("OpenAI is disabled. Use GroqLLMService.");
    }

    @Override
    public String generateCompletion(String prompt, int maxTokens, double temperature) {
        throw new UnsupportedOperationException("OpenAI is disabled. Use GroqLLMService.");
    }

    @Override
    public String getProviderName() {
        return "OpenAI (disabled)";
    }

    @Override
    public String getModelName() {
        return "disabled";
    }
}
