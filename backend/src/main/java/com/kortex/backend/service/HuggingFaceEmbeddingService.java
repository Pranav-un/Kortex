package com.kortex.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HuggingFace implementation of EmbeddingService.
 * Uses HuggingFace Inference API with sentence-transformer models.
 */
@Service
@Slf4j
public class HuggingFaceEmbeddingService implements EmbeddingService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiToken;
    private final String modelName;
    private final int embeddingDimension;
    private final int batchSize;

    public HuggingFaceEmbeddingService(
            @Value("${huggingface.api.url:https://api-inference.huggingface.co/pipeline/feature-extraction}") String apiUrl,
            @Value("${huggingface.api.token}") String apiToken,
            @Value("${huggingface.model.name:sentence-transformers/all-MiniLM-L6-v2}") String modelName,
            @Value("${huggingface.embedding.dimension:384}") int embeddingDimension,
            @Value("${huggingface.batch.size:32}") int batchSize) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        this.modelName = modelName;
        this.embeddingDimension = embeddingDimension;
        this.batchSize = batchSize;
        
        log.info("Initialized HuggingFaceEmbeddingService with model: {}", modelName);
    }

    @Override
    public double[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            Map<String, Object> requestBody = Map.of(
                    "inputs", text,
                    "options", Map.of("wait_for_model", true)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = apiUrl + "/" + modelName;
            ResponseEntity<double[]> response = restTemplate.postForEntity(url, request, double[].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to generate embedding: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Error calling HuggingFace API for embedding generation", e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    @Override
    public List<double[]> generateEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        // Filter out null or empty texts
        List<String> validTexts = texts.stream()
                .filter(text -> text != null && !text.trim().isEmpty())
                .collect(Collectors.toList());

        if (validTexts.isEmpty()) {
            return List.of();
        }

        // Process in batches to avoid API limits and memory issues
        List<double[]> allEmbeddings = new ArrayList<>();
        
        for (int i = 0; i < validTexts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, validTexts.size());
            List<String> batch = validTexts.subList(i, end);
            
            log.debug("Processing batch {}/{}: {} texts", 
                    (i / batchSize) + 1, 
                    (validTexts.size() + batchSize - 1) / batchSize, 
                    batch.size());
            
            List<double[]> batchEmbeddings = processBatch(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }

        return allEmbeddings;
    }

    /**
     * Process a single batch of texts.
     *
     * @param batch list of texts to process
     * @return list of embeddings
     */
    private List<double[]> processBatch(List<String> batch) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiToken);

            Map<String, Object> requestBody = Map.of(
                    "inputs", batch,
                    "options", Map.of("wait_for_model", true)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = apiUrl + "/" + modelName;
            ResponseEntity<double[][]> response = restTemplate.postForEntity(url, request, double[][].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return List.of(response.getBody());
            } else {
                throw new RuntimeException("Failed to generate batch embeddings: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Error calling HuggingFace API for batch embedding generation", e);
            // Fallback: process individually
            log.warn("Falling back to individual embedding generation for batch");
            return batch.stream()
                    .map(this::generateEmbedding)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
