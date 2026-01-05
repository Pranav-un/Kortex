package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for recent activity logs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentActivityResponse {
    
    /**
     * Recent document uploads.
     */
    private List<ActivityItem> recentUploads;
    
    /**
     * Total activity items returned.
     */
    private int totalItems;
    
    /**
     * DTO for individual activity item.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityItem {
        /**
         * Activity type.
         */
        private String activityType;
        
        /**
         * Document ID (if applicable).
         */
        private Long documentId;
        
        /**
         * Document name.
         */
        private String documentName;
        
        /**
         * File type.
         */
        private String fileType;
        
        /**
         * Activity timestamp.
         */
        private LocalDateTime timestamp;
        
        /**
         * Activity description.
         */
        private String description;
        
        /**
         * Additional metadata (e.g., word count, chunk count).
         */
        private String metadata;
    }
}
