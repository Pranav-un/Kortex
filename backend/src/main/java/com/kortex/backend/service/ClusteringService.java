package com.kortex.backend.service;

import com.kortex.backend.model.Document;
import com.kortex.backend.repository.DocumentChunkRepository;
import com.kortex.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for clustering and grouping related documents using embeddings.
 * Uses vector similarity to find document clusters and topics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClusteringService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final QdrantService qdrantService;

    /**
     * Find similar documents to a given document.
     * Uses average embedding of document chunks for similarity.
     *
     * @param userId the user ID
     * @param documentId the reference document ID
     * @param limit maximum number of similar documents to return
     * @return list of similar documents with similarity scores
     */
    public List<SimilarDocument> findSimilarDocuments(Long userId, Long documentId, int limit) {
        log.info("Finding similar documents to document {} for user {}", documentId, userId);

        try {
            // Verify document ownership
            Document document = documentRepository.findByIdAndOwnerId(documentId, userId)
                    .orElseThrow(() -> new RuntimeException("Document not found or access denied"));

            // Get all chunks for the document
            var chunks = documentChunkRepository.findByDocumentIdOrderByChunkOrderAsc(documentId);

            if (chunks.isEmpty()) {
                log.warn("No chunks found for document {}", documentId);
                return List.of();
            }

            // Get embedding of first chunk (representative)
            double[] queryEmbedding = chunks.get(0).getEmbedding();

            if (queryEmbedding == null) {
                log.warn("No embedding found for document {}", documentId);
                return List.of();
            }

            // Convert to float array for Qdrant
            float[] queryVector = new float[queryEmbedding.length];
            for (int i = 0; i < queryEmbedding.length; i++) {
                queryVector[i] = (float) queryEmbedding[i];
            }

            // Search Qdrant for similar chunks (get more results to find unique documents)
            List<QdrantService.SearchResult> results = qdrantService.search(userId, queryVector, limit * 5);

            // Group by document and calculate average similarity
            Map<Long, List<Float>> documentScores = new HashMap<>();
            for (QdrantService.SearchResult result : results) {
                Long docId = result.getDocumentId();
                
                // Skip the query document itself
                if (docId.equals(documentId)) {
                    continue;
                }

                documentScores.computeIfAbsent(docId, k -> new ArrayList<>()).add(result.getScore());
            }

            // Calculate average similarity per document and create results
            List<SimilarDocument> similarDocuments = documentScores.entrySet().stream()
                    .map(entry -> {
                        Long docId = entry.getKey();
                        List<Float> scores = entry.getValue();
                        float avgScore = (float) scores.stream()
                                .mapToDouble(Float::doubleValue)
                                .average()
                                .orElse(0.0);

                        Document doc = documentRepository.findById(docId).orElse(null);
                        if (doc == null) {
                            return null;
                        }

                        return SimilarDocument.builder()
                                .documentId(docId)
                                .filename(doc.getFilename())
                                .similarityScore(avgScore)
                                .tags(doc.getTags())
                                .summary(doc.getSummary())
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> Float.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                    .limit(limit)
                    .collect(Collectors.toList());

            log.info("Found {} similar documents to document {}", similarDocuments.size(), documentId);
            return similarDocuments;

        } catch (Exception e) {
            log.error("Error finding similar documents: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find similar documents: " + e.getMessage(), e);
        }
    }

    /**
     * Get document clusters/topics for a user.
     * Groups documents by tag similarity and vector similarity.
     *
     * @param userId the user ID
     * @param minClusterSize minimum documents per cluster
     * @return list of document clusters
     */
    public List<DocumentCluster> getDocumentClusters(Long userId, int minClusterSize) {
        log.info("Getting document clusters for user {}", userId);

        try {
            // Get all user documents with tags
            List<Document> documents = documentRepository.findByOwnerId(userId).stream()
                    .filter(doc -> doc.getTags() != null && !doc.getTags().isBlank())
                    .collect(Collectors.toList());

            if (documents.size() < minClusterSize) {
                log.info("Not enough documents for clustering: {}", documents.size());
                return List.of();
            }

            // Build tag frequency map across all documents
            Map<String, Integer> tagFrequency = new HashMap<>();
            for (Document doc : documents) {
                String[] tags = doc.getTags().split(",");
                for (String tag : tags) {
                    String cleanTag = tag.trim().toLowerCase();
                    tagFrequency.merge(cleanTag, 1, Integer::sum);
                }
            }

            // Find common tags (appearing in at least 2 documents)
            Set<String> commonTags = tagFrequency.entrySet().stream()
                    .filter(e -> e.getValue() >= minClusterSize)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            // Create clusters based on common tags
            Map<String, List<Document>> clusterMap = new HashMap<>();
            for (String tag : commonTags) {
                List<Document> clusterDocs = documents.stream()
                        .filter(doc -> doc.getTags().toLowerCase().contains(tag))
                        .collect(Collectors.toList());

                if (clusterDocs.size() >= minClusterSize) {
                    clusterMap.put(tag, clusterDocs);
                }
            }

            // Convert to DocumentCluster objects
            List<DocumentCluster> clusters = clusterMap.entrySet().stream()
                    .map(entry -> {
                        String topicName = capitalizeTag(entry.getKey());
                        List<Document> clusterDocs = entry.getValue();

                        return DocumentCluster.builder()
                                .topicName(topicName)
                                .documentCount(clusterDocs.size())
                                .documentIds(clusterDocs.stream()
                                        .map(Document::getId)
                                        .collect(Collectors.toList()))
                                .representativeDocuments(clusterDocs.stream()
                                        .limit(3)
                                        .map(doc -> ClusterDocument.builder()
                                                .documentId(doc.getId())
                                                .filename(doc.getFilename())
                                                .tags(doc.getTags())
                                                .summary(doc.getSummary() != null 
                                                        ? truncate(doc.getSummary(), 150) 
                                                        : null)
                                                .build())
                                        .collect(Collectors.toList()))
                                .build();
                    })
                    .sorted((a, b) -> Integer.compare(b.getDocumentCount(), a.getDocumentCount()))
                    .collect(Collectors.toList());

            log.info("Found {} document clusters for user {}", clusters.size(), userId);
            return clusters;

        } catch (Exception e) {
            log.error("Error getting document clusters: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get document clusters: " + e.getMessage(), e);
        }
    }

    /**
     * Get all unique tags for a user's documents.
     *
     * @param userId the user ID
     * @return map of tags to document count
     */
    public Map<String, Integer> getUserTags(Long userId) {
        List<Document> documents = documentRepository.findByOwnerId(userId);

        Map<String, Integer> tagCounts = new HashMap<>();
        for (Document doc : documents) {
            if (doc.getTags() != null && !doc.getTags().isBlank()) {
                String[] tags = doc.getTags().split(",");
                for (String tag : tags) {
                    String cleanTag = tag.trim();
                    if (!cleanTag.isEmpty()) {
                        tagCounts.merge(cleanTag, 1, Integer::sum);
                    }
                }
            }
        }

        return tagCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Capitalize first letter of tag.
     */
    private String capitalizeTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return tag;
        }
        return tag.substring(0, 1).toUpperCase() + tag.substring(1);
    }

    /**
     * Truncate text to specified length.
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Similar document result.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SimilarDocument {
        private Long documentId;
        private String filename;
        private Float similarityScore;
        private String tags;
        private String summary;
    }

    /**
     * Document cluster/topic.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DocumentCluster {
        private String topicName;
        private Integer documentCount;
        private List<Long> documentIds;
        private List<ClusterDocument> representativeDocuments;
    }

    /**
     * Document representation in cluster.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ClusterDocument {
        private Long documentId;
        private String filename;
        private String tags;
        private String summary;
    }
}
