package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for system health check response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthResponse {
    private String status; // "healthy", "degraded", "unhealthy"
    private LocalDateTime timestamp;
    private Long uptimeSeconds;
    
    // Component health
    private ComponentHealth database;
    private ComponentHealth vectorDatabase;
    private ComponentHealth storage;
    
    // Resource usage
    private ResourceUsage resources;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth {
        private String status; // "up", "down", "unknown"
        private String message;
        private Long responseTimeMs;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUsage {
        private Long usedMemoryMB;
        private Long maxMemoryMB;
        private Double memoryUsagePercentage;
        private Integer activeThreads;
        private Long diskUsedBytes;
        private Long diskTotalBytes;
        private String diskUsedFormatted;
        private String diskTotalFormatted;
    }
}
