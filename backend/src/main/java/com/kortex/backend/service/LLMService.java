package com.kortex.backend.service;

/**
 * Interface for Large Language Model (LLM) providers.
 * Supports multiple LLM backends (OpenAI, Groq, etc.)
 */
public interface LLMService {

    /**
     * Generate a completion for the given prompt.
     *
     * @param prompt the input prompt
     * @return the generated completion text
     */
    String generateCompletion(String prompt);

    /**
     * Generate a completion with custom parameters.
     *
     * @param prompt the input prompt
     * @param maxTokens maximum tokens to generate
     * @param temperature sampling temperature (0.0-2.0)
     * @return the generated completion text
     */
    String generateCompletion(String prompt, int maxTokens, double temperature);

    /**
     * Get the name of the LLM provider.
     *
     * @return provider name (e.g., "OpenAI", "Groq")
     */
    String getProviderName();

    /**
     * Get the model name being used.
     *
     * @return model name (e.g., "gpt-3.5-turbo", "llama3-8b-8192")
     */
    String getModelName();
}
