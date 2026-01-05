package com.kortex.backend.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Qdrant vector database client.
 */
@Configuration
@Slf4j
public class QdrantConfig {

    @Value("${qdrant.host}")
    private String host;

    @Value("${qdrant.port}")
    private int port;

    @Value("${qdrant.api.key:}")
    private String apiKey;

    @Value("${qdrant.use.tls}")
    private boolean useTls;

    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, useTls);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.withApiKey(apiKey);
        }

        QdrantClient client = new QdrantClient(builder.build());
        log.info("Initialized Qdrant client: {}:{} (TLS: {})", host, port, useTls);
        return client;
    }
}
