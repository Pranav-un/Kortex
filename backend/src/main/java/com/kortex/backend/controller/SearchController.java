package com.kortex.backend.controller;

import com.kortex.backend.dto.SearchRequest;
import com.kortex.backend.dto.SearchResponse;
import com.kortex.backend.model.Document;
import com.kortex.backend.repository.DocumentRepository;
import com.kortex.backend.security.UserDetailsImpl;
import com.kortex.backend.service.SemanticSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for semantic search operations.
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SemanticSearchService semanticSearchService;
    private final DocumentRepository documentRepository;

    /**
     * Perform semantic search across user's documents.
     *
     * @param searchRequest search parameters
     * @param authentication authenticated user
     * @return search results with matching chunks
     */
    @PostMapping
    public ResponseEntity<SearchResponse> search(
            @Valid @RequestBody SearchRequest searchRequest,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("User {} searching for: {}", userId, searchRequest.getQuery());

        List<SemanticSearchService.SearchResultWithChunk> results;

        if (searchRequest.getDocumentId() != null) {
            // Search within specific document
            results = semanticSearchService.searchInDocument(
                    userId,
                    searchRequest.getDocumentId(),
                    searchRequest.getQuery(),
                    searchRequest.getLimit()
            );
        } else {
            // Search across all documents
            results = semanticSearchService.search(
                    userId,
                    searchRequest.getQuery(),
                    searchRequest.getLimit()
            );
        }

        // Fetch document names for results
        List<Long> documentIds = results.stream()
                .map(SemanticSearchService.SearchResultWithChunk::getDocumentId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> documentNames = documentRepository.findAllById(documentIds)
                .stream()
                .collect(Collectors.toMap(Document::getId, Document::getFilename));

        // Convert to response DTO
        List<SearchResponse.SearchResultItem> resultItems = results.stream()
                .map(result -> SearchResponse.SearchResultItem.builder()
                        .chunkId(result.getChunkId())
                        .documentId(result.getDocumentId())
                        .documentName(documentNames.get(result.getDocumentId()))
                        .similarityScore(result.getScore())
                        .chunkText(result.getChunk().getChunkText())
                        .chunkOrder(result.getChunk().getChunkOrder())
                        .wordCount(result.getChunk().getWordCount())
                        .startPosition(result.getChunk().getStartPosition())
                        .endPosition(result.getChunk().getEndPosition())
                        .build())
                .collect(Collectors.toList());

        SearchResponse response = SearchResponse.builder()
                .query(searchRequest.getQuery())
                .resultsCount(resultItems.size())
                .results(resultItems)
                .build();

        log.info("Found {} results for query: {}", resultItems.size(), searchRequest.getQuery());
        return ResponseEntity.ok(response);
    }

    /**
     * Quick search endpoint with query parameter.
     *
     * @param query search query
     * @param limit optional result limit (default 10)
     * @param authentication authenticated user
     * @return search results
     */
    @GetMapping
    public ResponseEntity<SearchResponse> quickSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") Integer limit,
            Authentication authentication) {
        
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .limit(Math.min(limit, 100)) // Cap at 100
                .build();

        return search(searchRequest, authentication);
    }
}
