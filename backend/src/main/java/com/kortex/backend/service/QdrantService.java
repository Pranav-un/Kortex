package com.kortex.backend.service;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CollectionInfo;
import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import io.qdrant.client.grpc.Points.DeletePoints;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.FieldCondition;
import io.qdrant.client.grpc.Points.Match;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.PointsSelector;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.UpsertPoints;
import io.qdrant.client.grpc.Points.Vectors;
import io.qdrant.client.grpc.Points.Vector;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import io.qdrant.client.grpc.Points.Condition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Service for interacting with Qdrant vector database.
 * Manages collections, stores vectors with metadata, and performs similarity search.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantService {

    private final QdrantClient qdrantClient;
    private final EmbeddingService embeddingService;

    @Value("${qdrant.collection.prefix}")
    private String collectionPrefix;

    /**
     * Get collection name for a user.
     * Collections are namespaced per user for data isolation.
     *
     * @param userId the user ID
     * @return collection name
     */
    public String getCollectionName(Long userId) {
        return collectionPrefix + "_user_" + userId;
    }

    /**
     * Create a collection for a user if it doesn't exist.
     *
     * @param userId the user ID
     * @throws RuntimeException if collection creation fails
     */
    public void ensureCollectionExists(Long userId) {
        String collectionName = getCollectionName(userId);
        
        try {
            // Check if collection exists
            CollectionInfo info = qdrantClient.getCollectionInfoAsync(collectionName).get();
            log.debug("Collection {} already exists", collectionName);
        } catch (Exception e) {
            // Collection doesn't exist, create it
            try {
                int vectorSize = embeddingService.getEmbeddingDimension();
                
                VectorParams vectorParams = VectorParams.newBuilder()
                        .setSize(vectorSize)
                        .setDistance(Distance.Cosine)
                        .build();

                CreateCollection createCollection = CreateCollection.newBuilder()
                        .setCollectionName(collectionName)
                        .setVectorsConfig(VectorsConfig.newBuilder()
                                .setParams(vectorParams)
                                .build())
                        .build();

                qdrantClient.createCollectionAsync(createCollection).get();
                log.info("Created Qdrant collection: {} with vector size: {}", collectionName, vectorSize);
            } catch (InterruptedException | ExecutionException ex) {
                log.error("Failed to create collection: {}", collectionName, ex);
                throw new RuntimeException("Failed to create Qdrant collection", ex);
            }
        }
    }

    /**
     * Store a vector with metadata in Qdrant.
     *
     * @param userId the user ID (for collection namespace)
     * @param chunkId the chunk ID (used as point ID)
     * @param documentId the document ID (stored in payload)
     * @param embedding the embedding vector
     * @param metadata additional metadata to store
     */
    public void storeVector(Long userId, Long chunkId, Long documentId, double[] embedding, Map<String, Object> metadata) {
        ensureCollectionExists(userId);
        
        String collectionName = getCollectionName(userId);
        
        try {
            // Convert double[] to List<Float>
            List<Float> vector = Arrays.stream(embedding)
                    .mapToObj(d -> (float) d)
                    .collect(Collectors.toList());

            // Build payload with metadata (Qdrant JsonWithInt.Value)
            Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
            payload.put("documentId", io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setIntegerValue(documentId).build());
            payload.put("chunkId", io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setIntegerValue(chunkId).build());
            
            // Add additional metadata
            if (metadata != null) {
                metadata.forEach((key, value) -> {
                    if (value instanceof String) {
                        payload.put(key, io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setStringValue((String) value).build());
                    } else if (value instanceof Integer) {
                        payload.put(key, io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setIntegerValue((Integer) value).build());
                    } else if (value instanceof Long) {
                        payload.put(key, io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setIntegerValue((Long) value).build());
                    } else if (value instanceof Boolean) {
                        payload.put(key, io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setBoolValue((Boolean) value).build());
                    }
                });
            }

            // Create point
            PointStruct point = PointStruct.newBuilder()
                    .setId(PointId.newBuilder().setNum(chunkId).build())
                    .setVectors(Vectors.newBuilder().setVector(Vector.newBuilder().addAllData(vector).build()).build())
                    .putAllPayload(payload)
                    .build();

            // Upsert point
            UpsertPoints upsertPoints = UpsertPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addPoints(point)
                    .build();

            qdrantClient.upsertAsync(upsertPoints).get();
            log.debug("Stored vector for chunk {} in collection {}", chunkId, collectionName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to store vector for chunk {}", chunkId, e);
            throw new RuntimeException("Failed to store vector in Qdrant", e);
        }
    }

    /**
     * Store multiple vectors in batch.
     *
     * @param userId the user ID
     * @param vectors list of vectors with their metadata
     */
    public void storeVectorsBatch(Long userId, List<VectorData> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return;
        }

        ensureCollectionExists(userId);
        String collectionName = getCollectionName(userId);

        try {
            List<PointStruct> points = vectors.stream()
                    .map(vectorData -> {
                        List<Float> vector = Arrays.stream(vectorData.getEmbedding())
                                .mapToObj(d -> (float) d)
                                .collect(Collectors.toList());

                        Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new HashMap<>();
                        payload.put("documentId", io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setIntegerValue(vectorData.documentId).build());
                        payload.put("chunkId", io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setIntegerValue(vectorData.chunkId).build());

                        if (vectorData.getMetadata() != null) {
                            vectorData.getMetadata().forEach((key, value) -> {
                                if (value instanceof String) {
                                    payload.put(key, io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setStringValue((String) value).build());
                                } else if (value instanceof Integer) {
                                    payload.put(key, io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setIntegerValue((Integer) value).build());
                                } else if (value instanceof Long) {
                                    payload.put(key, io.qdrant.client.grpc.JsonWithInt.Value.newBuilder().setIntegerValue((Long) value).build());
                                }
                            });
                        }

                        return PointStruct.newBuilder()
                                .setId(PointId.newBuilder().setNum(vectorData.getChunkId()).build())
                                .setVectors(Vectors.newBuilder().setVector(Vector.newBuilder().addAllData(vector).build()).build())
                                .putAllPayload(payload)
                                .build();
                    })
                    .collect(Collectors.toList());

            UpsertPoints upsertPoints = UpsertPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllPoints(points)
                    .build();

            qdrantClient.upsertAsync(upsertPoints).get();
            log.info("Stored {} vectors in batch for collection {}", vectors.size(), collectionName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to store vectors in batch", e);
            throw new RuntimeException("Failed to store vectors in Qdrant", e);
        }
    }

    /**
     * Delete vectors for a specific document.
     *
     * @param userId the user ID
     * @param documentId the document ID
     */
    public void deleteVectorsByDocumentId(Long userId, Long documentId) {
        String collectionName = getCollectionName(userId);

        try {
            // Create filter for documentId
            Filter filter = Filter.newBuilder()
                    .addMust(Condition.newBuilder()
                            .setField(FieldCondition.newBuilder()
                                    .setKey("documentId")
                                    .setMatch(Match.newBuilder()
                                            .setInteger(documentId)
                                            .build())
                                    .build())
                            .build())
                    .build();

            DeletePoints deletePoints = DeletePoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setPoints(PointsSelector.newBuilder()
                            .setFilter(filter)
                            .build())
                    .build();

            qdrantClient.deleteAsync(deletePoints).get();
            log.info("Deleted vectors for document {} from collection {}", documentId, collectionName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Delete interrupted for document {}", documentId, e);
            throw new RuntimeException("Failed to delete vectors from Qdrant", e);
        } catch (ExecutionException e) {
            // Gracefully handle missing collection without hard dependency on io.grpc at compile time
            Throwable cause = e.getCause();
            String causeClass = cause != null ? cause.getClass().getName() : "";
            String causeMessage = cause != null && cause.getMessage() != null ? cause.getMessage() : "";
            if ("io.grpc.StatusRuntimeException".equals(causeClass) && causeMessage.contains("NOT_FOUND")) {
                log.warn("Qdrant collection {} not found when deleting document {} — skipping delete", collectionName, documentId);
                return;
            }
            log.error("Failed to delete vectors for document {}", documentId, e);
            throw new RuntimeException("Failed to delete vectors from Qdrant", e);
        }
    }

    /**
     * Search for similar vectors using cosine similarity.
     *
     * @param userId the user ID
     * @param queryEmbedding the query embedding vector
     * @param limit maximum number of results
     * @return list of search results with scores
     */
    public List<SearchResult> search(Long userId, double[] queryEmbedding, int limit) {
        String collectionName = getCollectionName(userId);

        // Ensure the collection exists before searching to avoid NOT_FOUND errors
        ensureCollectionExists(userId);

        try {
            List<Float> queryVector = Arrays.stream(queryEmbedding)
                    .mapToObj(d -> (float) d)
                    .collect(Collectors.toList());

            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(queryVector)
                    .setLimit(limit)
                    .setWithPayload(WithPayloadSelector.newBuilder()
                            .setEnable(true)
                            .build())
                    .build();

            List<ScoredPoint> scoredPoints = qdrantClient.searchAsync(searchPoints).get();

            return scoredPoints.stream()
                    .map(point -> {
                        Long chunkId = point.getId().getNum();
                        float score = point.getScore();
                        
                        Map<String, Object> payload = new HashMap<>();
                        point.getPayloadMap().forEach((key, value) -> {
                            if (value.hasIntegerValue()) {
                                payload.put(key, value.getIntegerValue());
                            } else if (value.hasStringValue()) {
                                payload.put(key, value.getStringValue());
                            } else if (value.hasBoolValue()) {
                                payload.put(key, value.getBoolValue());
                            }
                        });

                        return new SearchResult(chunkId, score, payload);
                    })
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to search vectors in collection {}", collectionName, e);
            throw new RuntimeException("Failed to search in Qdrant", e);
        }
    }

    /**
     * Delete entire collection for a user.
     *
     * @param userId the user ID
     */
    public void deleteCollection(Long userId) {
        String collectionName = getCollectionName(userId);

        try {
            qdrantClient.deleteCollectionAsync(collectionName).get();
            log.info("Deleted collection {}", collectionName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to delete collection {}", collectionName, e);
            throw new RuntimeException("Failed to delete collection from Qdrant", e);
        }
    }

    /**
     * Data class for batch vector storage.
     */
    public static class VectorData {
        private final Long chunkId;
        private final Long documentId;
        private final double[] embedding;
        private final Map<String, Object> metadata;

        public VectorData(Long chunkId, Long documentId, double[] embedding, Map<String, Object> metadata) {
            this.chunkId = chunkId;
            this.documentId = documentId;
            this.embedding = embedding;
            this.metadata = metadata;
        }

        public Long getChunkId() {
            return chunkId;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public double[] getEmbedding() {
            return embedding;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * Data class for search results.
     */
    public static class SearchResult {
        private final Long chunkId;
        private final float score;
        private final Map<String, Object> payload;

        public SearchResult(Long chunkId, float score, Map<String, Object> payload) {
            this.chunkId = chunkId;
            this.score = score;
            this.payload = payload;
        }

        public Long getChunkId() {
            return chunkId;
        }

        public float getScore() {
            return score;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public Long getDocumentId() {
            Object docId = payload.get("documentId");
            return docId instanceof Long ? (Long) docId : null;
        }
    }
}
