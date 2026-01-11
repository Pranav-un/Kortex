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
    private final String embeddingsUrl; // Optional explicit embeddings base (e.g., local TEI)
    private final int embeddingDimension;
    private final int batchSize;

        public HuggingFaceEmbeddingService(
            @Value("${huggingface.api.url:https://router.huggingface.co/models}") String apiUrl,
            @Value("${huggingface.api.token}") String apiToken,
            @Value("${huggingface.model.name:sentence-transformers/all-MiniLM-L6-v2}") String modelName,
            @Value("${huggingface.embeddings.url:}") String embeddingsUrl,
            @Value("${huggingface.embedding.dimension:384}") int embeddingDimension,
            @Value("${huggingface.batch.size:32}") int batchSize) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        this.modelName = modelName;
        this.embeddingsUrl = embeddingsUrl;
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
            if (apiToken != null && !apiToken.isBlank()) {
                headers.setBearerAuth(apiToken);
            }
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            Map<String, Object> requestBody = Map.of(
                    "inputs", text,
                    "options", Map.of("wait_for_model", true)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // If explicit embeddings URL is configured (e.g., local TEI), try it first
            String teiUrl = resolveEmbeddingsEndpoint();
            if (teiUrl != null) {
                log.info("Using explicit embeddings endpoint: {}", teiUrl);
                Map<String, Object> teiBody = Map.of("input", List.of(text));
                HttpEntity<Map<String, Object>> teiRequest = new HttpEntity<>(teiBody, headers);
                try {
                    ResponseEntity<Object> teiResponse = restTemplate.postForEntity(teiUrl, teiRequest, Object.class);
                    if (teiResponse.getStatusCode() == HttpStatus.OK && teiResponse.getBody() != null) {
                        return parseSingleTeiEmbedding(teiResponse.getBody());
                    }
                    log.warn("TEI embeddings endpoint returned status: {}", teiResponse.getStatusCode());
                } catch (org.springframework.web.client.HttpClientErrorException teiEx) {
                    log.warn("Explicit embeddings endpoint failed ({}), falling back to router endpoints.", teiEx.getStatusCode());
                }
            }

            // Primary: Inference API models endpoint
            String primaryUrl = apiUrl + "/" + modelName;
            try {
                ResponseEntity<Object> response = restTemplate.postForEntity(primaryUrl, request, Object.class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return parseSingleEmbedding(response.getBody());
                }
                throw new RuntimeException("Failed to generate embedding: " + response.getStatusCode());
            } catch (org.springframework.web.client.HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.GONE) {
                    // Fallback 1: router pipeline feature-extraction endpoint
                    String routerPipelineBase = "https://router.huggingface.co/pipeline/feature-extraction";
                    String routerPipelineUrl = routerPipelineBase + "/" + modelName;
                    log.warn("Primary HF models endpoint {} failed ({}). Trying router pipeline: {}", primaryUrl, ex.getStatusCode(), routerPipelineUrl);
                    try {
                        ResponseEntity<Object> response = restTemplate.postForEntity(routerPipelineUrl, request, Object.class);
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            return parseSingleEmbedding(response.getBody());
                        }
                        log.warn("HF router pipeline endpoint returned status: {}", response.getStatusCode());
                    } catch (org.springframework.web.client.HttpClientErrorException ex2) {
                        if (!(ex2.getStatusCode() == HttpStatus.NOT_FOUND || ex2.getStatusCode() == HttpStatus.GONE)) {
                            throw ex2;
                        }
                        log.warn("HF router pipeline endpoint failed ({}). Trying Inference API pipeline next.", ex2.getStatusCode());
                    }

                    // Fallback 2: direct Inference API pipeline feature-extraction endpoint
                    String inferencePipelineBase = "https://api-inference.huggingface.co/pipeline/feature-extraction";
                    String inferencePipelineUrl = inferencePipelineBase + "/" + modelName;
                    log.warn("Trying HF Inference API pipeline: {}", inferencePipelineUrl);
                    try {
                        ResponseEntity<Object> response = restTemplate.postForEntity(inferencePipelineUrl, request, Object.class);
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            return parseSingleEmbedding(response.getBody());
                        }
                        log.warn("HF Inference API pipeline returned status: {}", response.getStatusCode());
                    } catch (org.springframework.web.client.HttpClientErrorException ex3) {
                        if (!(ex3.getStatusCode() == HttpStatus.NOT_FOUND || ex3.getStatusCode() == HttpStatus.GONE)) {
                            throw ex3;
                        }
                        log.warn("HF Inference API pipeline failed ({}). Trying embeddings endpoint next.", ex3.getStatusCode());
                    }

                    // Fallback 3: router embeddings endpoint
                    String embeddingsBase = "https://router.huggingface.co/embeddings";
                    String embeddingsUrl = embeddingsBase + "/" + modelName;
                    log.warn("Trying router embeddings endpoint: {}", embeddingsUrl);
                    try {
                        ResponseEntity<Object> response = restTemplate.postForEntity(embeddingsUrl, request, Object.class);
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            return parseSingleEmbedding(response.getBody());
                        }
                        log.warn("Router embeddings endpoint returned status: {}. Trying Inference API embeddings.", response.getStatusCode());
                    } catch (org.springframework.web.client.HttpClientErrorException ex4) {
                        log.warn("Router embeddings endpoint failed ({}). Trying Inference API embeddings next.", ex4.getStatusCode());
                    }

                    // Fallback 4: direct Inference API embeddings endpoint
                    String inferenceEmbeddingsBase = "https://api-inference.huggingface.co/embeddings";
                    String inferenceEmbeddingsUrl = inferenceEmbeddingsBase + "/" + modelName;
                    log.warn("Trying HF Inference API embeddings: {}", inferenceEmbeddingsUrl);
                    ResponseEntity<Object> response2 = restTemplate.postForEntity(inferenceEmbeddingsUrl, request, Object.class);
                    if (response2.getStatusCode() == HttpStatus.OK && response2.getBody() != null) {
                        return parseSingleEmbedding(response2.getBody());
                    }
                    throw new RuntimeException("Failed to generate embedding via HF Inference API embeddings endpoint: " + response2.getStatusCode());
                }
                throw ex;
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
            if (apiToken != null && !apiToken.isBlank()) {
                headers.setBearerAuth(apiToken);
            }
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            Map<String, Object> requestBody = Map.of(
                    "inputs", batch,
                    "options", Map.of("wait_for_model", true)
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // If explicit embeddings URL is configured (e.g., local TEI), try it first for batch
            String teiUrl = resolveEmbeddingsEndpoint();
            if (teiUrl != null) {
                log.info("Using explicit embeddings endpoint (batch): {}", teiUrl);
                Map<String, Object> teiBody = Map.of("input", batch);
                HttpEntity<Map<String, Object>> teiRequest = new HttpEntity<>(teiBody, headers);
                try {
                    ResponseEntity<Object> teiResponse = restTemplate.postForEntity(teiUrl, teiRequest, Object.class);
                    if (teiResponse.getStatusCode() == HttpStatus.OK && teiResponse.getBody() != null) {
                        return parseBatchTeiEmbeddings(teiResponse.getBody());
                    }
                    log.warn("TEI embeddings endpoint (batch) returned status: {}", teiResponse.getStatusCode());
                } catch (org.springframework.web.client.HttpClientErrorException teiEx) {
                    log.warn("Explicit embeddings endpoint (batch) failed ({}), falling back to router endpoints.", teiEx.getStatusCode());
                }
            }

            // Primary: Inference API models endpoint
            String primaryUrl = apiUrl + "/" + modelName;
            try {
                ResponseEntity<Object> response = restTemplate.postForEntity(primaryUrl, request, Object.class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    return parseBatchEmbeddings(response.getBody());
                }
                throw new RuntimeException("Failed to generate batch embeddings: " + response.getStatusCode());
            } catch (org.springframework.web.client.HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND || ex.getStatusCode() == HttpStatus.GONE) {
                    // Fallback 1: router pipeline feature-extraction endpoint
                    String routerPipelineBase = "https://router.huggingface.co/pipeline/feature-extraction";
                    String routerPipelineUrl = routerPipelineBase + "/" + modelName;
                    log.warn("Primary HF models endpoint {} failed (batch, {}). Trying router pipeline: {}", primaryUrl, ex.getStatusCode(), routerPipelineUrl);
                    try {
                        ResponseEntity<Object> response = restTemplate.postForEntity(routerPipelineUrl, request, Object.class);
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            return parseBatchEmbeddings(response.getBody());
                        }
                        log.warn("HF router pipeline endpoint (batch) returned status: {}", response.getStatusCode());
                    } catch (org.springframework.web.client.HttpClientErrorException ex2) {
                        if (!(ex2.getStatusCode() == HttpStatus.NOT_FOUND || ex2.getStatusCode() == HttpStatus.GONE)) {
                            throw ex2;
                        }
                        log.warn("HF router pipeline endpoint (batch) failed ({}). Trying Inference API pipeline next.", ex2.getStatusCode());
                    }

                    // Fallback 2: direct Inference API pipeline feature-extraction endpoint
                    String inferencePipelineBase = "https://api-inference.huggingface.co/pipeline/feature-extraction";
                    String inferencePipelineUrl = inferencePipelineBase + "/" + modelName;
                    log.warn("Trying HF Inference API pipeline (batch): {}", inferencePipelineUrl);
                    try {
                        ResponseEntity<Object> response = restTemplate.postForEntity(inferencePipelineUrl, request, Object.class);
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            return parseBatchEmbeddings(response.getBody());
                        }
                        log.warn("HF Inference API pipeline (batch) returned status: {}", response.getStatusCode());
                    } catch (org.springframework.web.client.HttpClientErrorException ex3) {
                        if (!(ex3.getStatusCode() == HttpStatus.NOT_FOUND || ex3.getStatusCode() == HttpStatus.GONE)) {
                            throw ex3;
                        }
                        log.warn("HF Inference API pipeline (batch) failed ({}). Trying embeddings endpoint next.", ex3.getStatusCode());
                    }

                    // Fallback 3: router embeddings endpoint
                    String embeddingsBase = "https://router.huggingface.co/embeddings";
                    String embeddingsUrl = embeddingsBase + "/" + modelName;
                    log.warn("Trying router embeddings endpoint (batch): {}", embeddingsUrl);
                    try {
                        ResponseEntity<Object> response = restTemplate.postForEntity(embeddingsUrl, request, Object.class);
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            return parseBatchEmbeddings(response.getBody());
                        }
                        log.warn("Router embeddings endpoint (batch) returned status: {}. Trying Inference API embeddings.", response.getStatusCode());
                    } catch (org.springframework.web.client.HttpClientErrorException ex4) {
                        log.warn("Router embeddings endpoint (batch) failed ({}). Trying Inference API embeddings next.", ex4.getStatusCode());
                    }

                    // Fallback 4: direct Inference API embeddings endpoint
                    String inferenceEmbeddingsBase = "https://api-inference.huggingface.co/embeddings";
                    String inferenceEmbeddingsUrl = inferenceEmbeddingsBase + "/" + modelName;
                    log.warn("Trying HF Inference API embeddings (batch): {}", inferenceEmbeddingsUrl);
                    ResponseEntity<Object> response2 = restTemplate.postForEntity(inferenceEmbeddingsUrl, request, Object.class);
                    if (response2.getStatusCode() == HttpStatus.OK && response2.getBody() != null) {
                        return parseBatchEmbeddings(response2.getBody());
                    }
                    throw new RuntimeException("Failed to generate batch embeddings via HF Inference API embeddings endpoint: " + response2.getStatusCode());
                }
                throw ex;
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

    /**
     * Resolve the full embeddings endpoint when an explicit base URL is provided.
     * If the configured URL already points to the embeddings path, use it as-is; otherwise append "/embeddings".
     */
    private String resolveEmbeddingsEndpoint() {
        if (embeddingsUrl == null || embeddingsUrl.isBlank()) {
            return null;
        }
        if (embeddingsUrl.endsWith("/embeddings")) {
            return embeddingsUrl;
        }
        return embeddingsUrl + "/embeddings";
    }

    // Parse a single embedding vector from HF responses which may be 1D or nested arrays
    private double[] parseSingleEmbedding(Object body) {
        if (body instanceof List<?> list) {
            // If first element is a List => pick the first row (sentence-level)
            if (!list.isEmpty() && list.get(0) instanceof List<?> inner) {
                return toDoubleArray(inner);
            }
            // Otherwise the list itself is the vector
            return toDoubleArray(list);
        }
        // Try to handle primitive array mapping fallback
        if (body instanceof double[] arr) {
            return arr;
        }
        throw new RuntimeException("Unexpected HF embedding response format (single).");
    }

    // Parse TEI single embedding: { "data": [{ "embedding": [..] }] }
    private double[] parseSingleTeiEmbedding(Object body) {
        if (body instanceof Map<?, ?> map) {
            Object dataObj = map.get("data");
            if (dataObj instanceof List<?> data && !data.isEmpty()) {
                Object first = data.get(0);
                if (first instanceof Map<?, ?> item) {
                    Object emb = item.get("embedding");
                    if (emb instanceof List<?> embList) {
                        return toDoubleArray(embList);
                    }
                }
            }
        }
        // Fallback to generic parser if TEI shape differs
        return parseSingleEmbedding(body);
    }

    // Parse batch embeddings from HF responses which may be 2D or mixed
    private List<double[]> parseBatchEmbeddings(Object body) {
        if (body instanceof List<?> outer) {
            List<double[]> result = new ArrayList<>();
            for (Object row : outer) {
                if (row instanceof List<?> inner) {
                    // If nested lists (e.g., token-level), take the first sentence-level vector
                    if (!inner.isEmpty() && inner.get(0) instanceof List<?>) {
                        result.add(toDoubleArray((List<?>) inner.get(0)));
                    } else {
                        result.add(toDoubleArray(inner));
                    }
                } else if (row instanceof double[] darr) {
                    result.add(darr);
                } else {
                    throw new RuntimeException("Unexpected HF embedding response row format (batch).");
                }
            }
            return result;
        }
        if (body instanceof double[][] arr2d) {
            return List.of(arr2d);
        }
        throw new RuntimeException("Unexpected HF embedding response format (batch).");
    }

    // Parse TEI batch embeddings: { "data": [{ "embedding": [..] }, ...] }
    private List<double[]> parseBatchTeiEmbeddings(Object body) {
        if (body instanceof Map<?, ?> map) {
            Object dataObj = map.get("data");
            if (dataObj instanceof List<?> data && !data.isEmpty()) {
                List<double[]> result = new ArrayList<>();
                for (Object itemObj : data) {
                    if (itemObj instanceof Map<?, ?> item) {
                        Object emb = item.get("embedding");
                        if (emb instanceof List<?> embList) {
                            result.add(toDoubleArray(embList));
                        } else {
                            throw new RuntimeException("Unexpected TEI embedding row format (batch).");
                        }
                    } else {
                        throw new RuntimeException("Unexpected TEI embedding item type (batch).");
                    }
                }
                return result;
            }
        }
        // Fallback to generic parser if TEI shape differs
        return parseBatchEmbeddings(body);
    }

    private double[] toDoubleArray(List<?> floats) {
        double[] out = new double[floats.size()];
        for (int i = 0; i < floats.size(); i++) {
            Object v = floats.get(i);
            if (v instanceof Number n) {
                out[i] = n.doubleValue();
            } else {
                throw new RuntimeException("Non-numeric value in embedding vector");
            }
        }
        return out;
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
