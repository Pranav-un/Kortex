package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTO for upload statistics analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadStatisticsResponse {
    
    /**
     * Documents uploaded over time (date -> count).
     */
    private List<UploadByDate> uploadsByDate;
    
    /**
     * Documents uploaded by month (YYYY-MM -> count).
     */
    private List<UploadByMonth> uploadsByMonth;
    
    /**
     * File type distribution with percentages.
     */
    private List<FileTypeStats> fileTypeStats;
    
    /**
     * Average document size in bytes.
     */
    private long averageSizeBytes;
    
    /**
     * Average document size (human-readable).
     */
    private String averageSizeFormatted;
    
    /**
     * Largest document size in bytes.
     */
    private long largestDocumentBytes;
    
    /**
     * Largest document name.
     */
    private String largestDocumentName;
    
    /**
     * Total uploads in the last 7 days.
     */
    private long uploadsLast7Days;
    
    /**
     * Total uploads in the last 30 days.
     */
    private long uploadsLast30Days;
    
    /**
     * DTO for uploads by date.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadByDate {
        private LocalDate date;
        private long count;
    }
    
    /**
     * DTO for uploads by month.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadByMonth {
        private String month;  // YYYY-MM format
        private long count;
    }
    
    /**
     * DTO for file type statistics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FileTypeStats {
        private String fileType;
        private long count;
        private double percentage;
    }
}
