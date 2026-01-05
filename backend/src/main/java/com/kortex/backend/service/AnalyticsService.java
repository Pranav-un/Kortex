package com.kortex.backend.service;

import com.kortex.backend.dto.*;
import com.kortex.backend.model.Document;
import com.kortex.backend.repository.DocumentChunkRepository;
import com.kortex.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating analytics and statistics for the dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private static final int AVERAGE_READING_SPEED_WPM = 200; // words per minute
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    /**
     * Get overview statistics for user's documents.
     */
    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getOverviewStatistics(Long userId) {
        List<Document> documents = documentRepository.findByOwnerId(userId);

        long totalDocuments = documents.size();
        long totalWords = 0;
        long totalStorage = 0;
        long documentsWithText = 0;
        long documentsWithSummaries = 0;
        long documentsWithTags = 0;
        Map<String, Long> fileTypeDistribution = new HashMap<>();

        for (Document doc : documents) {
            // Count words from extracted text
            if (doc.getExtractedText() != null && !doc.getExtractedText().isBlank()) {
                documentsWithText++;
                String[] words = doc.getExtractedText().split("\\s+");
                totalWords += words.length;
            }

            // Storage
            totalStorage += doc.getSize();

            // Summaries
            if (doc.getSummary() != null && !doc.getSummary().isBlank()) {
                documentsWithSummaries++;
            }

            // Tags
            if (doc.getTags() != null && !doc.getTags().isBlank()) {
                documentsWithTags++;
            }

            // File type distribution
            String fileType = getFileTypeFromContentType(doc.getFileType());
            fileTypeDistribution.merge(fileType, 1L, Long::sum);
        }

        // Count documents with embeddings (have chunks)
        long documentsWithEmbeddings = documents.stream()
                .filter(doc -> documentChunkRepository.countByDocumentId(doc.getId()) > 0)
                .count();

        // Calculate reading time (words / 200 wpm)
        long totalReadingMinutes = totalWords / AVERAGE_READING_SPEED_WPM;
        double averageWordsPerDoc = totalDocuments > 0 ? (double) totalWords / totalDocuments : 0;
        double averageReadingMinutes = totalDocuments > 0 ? (double) totalReadingMinutes / totalDocuments : 0;

        return AnalyticsOverviewResponse.builder()
                .totalDocuments(totalDocuments)
                .totalWords(totalWords)
                .averageWordsPerDocument(Math.round(averageWordsPerDoc * 100.0) / 100.0)
                .totalReadingTimeMinutes(totalReadingMinutes)
                .averageReadingTimeMinutes(Math.round(averageReadingMinutes * 100.0) / 100.0)
                .totalStorageBytes(totalStorage)
                .totalStorageFormatted(formatBytes(totalStorage))
                .documentsWithText(documentsWithText)
                .documentsWithEmbeddings(documentsWithEmbeddings)
                .documentsWithSummaries(documentsWithSummaries)
                .documentsWithTags(documentsWithTags)
                .fileTypeDistribution(fileTypeDistribution)
                .build();
    }

    /**
     * Get keyword frequency statistics from document tags.
     */
    @Transactional(readOnly = true)
    public KeywordFrequencyResponse getKeywordFrequency(Long userId, Integer limit) {
        List<Document> documents = documentRepository.findByOwnerId(userId);

        // Extract all keywords from tags
        Map<String, Long> keywordCounts = new HashMap<>();
        long totalDocuments = documents.size();

        for (Document doc : documents) {
            if (doc.getTags() != null && !doc.getTags().isBlank()) {
                String[] tags = doc.getTags().split(",");
                for (String tag : tags) {
                    String cleanTag = tag.trim().toLowerCase();
                    if (!cleanTag.isEmpty()) {
                        keywordCounts.merge(cleanTag, 1L, Long::sum);
                    }
                }
            }
        }

        // Sort by frequency and take top N
        int limitValue = limit != null ? limit : 20;
        List<KeywordFrequencyResponse.KeywordCount> keywords = keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limitValue)
                .map(entry -> KeywordFrequencyResponse.KeywordCount.builder()
                        .keyword(entry.getKey())
                        .documentCount(entry.getValue())
                        .frequencyPercentage(totalDocuments > 0 ? 
                            Math.round((double) entry.getValue() / totalDocuments * 10000.0) / 100.0 : 0)
                        .build())
                .collect(Collectors.toList());

        return KeywordFrequencyResponse.builder()
                .keywords(keywords)
                .totalUniqueKeywords(keywordCounts.size())
                .build();
    }

    /**
     * Get upload statistics over time and by file type.
     */
    @Transactional(readOnly = true)
    public UploadStatisticsResponse getUploadStatistics(Long userId) {
        List<Document> documents = documentRepository.findByOwnerId(userId);

        // Uploads by date (last 30 days)
        Map<LocalDate, Long> uploadsByDateMap = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        for (Document doc : documents) {
            LocalDate uploadDate = doc.getUploadTime().toLocalDate();
            uploadsByDateMap.merge(uploadDate, 1L, Long::sum);
        }

        List<UploadStatisticsResponse.UploadByDate> uploadsByDate = uploadsByDateMap.entrySet().stream()
                .filter(entry -> !entry.getKey().isBefore(thirtyDaysAgo))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> UploadStatisticsResponse.UploadByDate.builder()
                        .date(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // Uploads by month
        Map<String, Long> uploadsByMonthMap = new HashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (Document doc : documents) {
            String month = doc.getUploadTime().format(monthFormatter);
            uploadsByMonthMap.merge(month, 1L, Long::sum);
        }

        List<UploadStatisticsResponse.UploadByMonth> uploadsByMonth = uploadsByMonthMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> UploadStatisticsResponse.UploadByMonth.builder()
                        .month(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        // File type statistics
        Map<String, Long> fileTypeCounts = new HashMap<>();
        for (Document doc : documents) {
            String fileType = getFileTypeFromContentType(doc.getFileType());
            fileTypeCounts.merge(fileType, 1L, Long::sum);
        }

        long totalDocs = documents.size();
        List<UploadStatisticsResponse.FileTypeStats> fileTypeStats = fileTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> UploadStatisticsResponse.FileTypeStats.builder()
                        .fileType(entry.getKey())
                        .count(entry.getValue())
                        .percentage(totalDocs > 0 ? 
                            Math.round((double) entry.getValue() / totalDocs * 10000.0) / 100.0 : 0)
                        .build())
                .collect(Collectors.toList());

        // Average and largest document size
        long averageSize = totalDocs > 0 ? documents.stream()
                .mapToLong(Document::getSize)
                .sum() / totalDocs : 0;

        Document largestDoc = documents.stream()
                .max(Comparator.comparingLong(Document::getSize))
                .orElse(null);

        // Recent uploads count
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime thirtyDaysAgoTime = LocalDateTime.now().minusDays(30);

        long uploadsLast7Days = documents.stream()
                .filter(doc -> doc.getUploadTime().isAfter(sevenDaysAgo))
                .count();

        long uploadsLast30Days = documents.stream()
                .filter(doc -> doc.getUploadTime().isAfter(thirtyDaysAgoTime))
                .count();

        return UploadStatisticsResponse.builder()
                .uploadsByDate(uploadsByDate)
                .uploadsByMonth(uploadsByMonth)
                .fileTypeStats(fileTypeStats)
                .averageSizeBytes(averageSize)
                .averageSizeFormatted(formatBytes(averageSize))
                .largestDocumentBytes(largestDoc != null ? largestDoc.getSize() : 0)
                .largestDocumentName(largestDoc != null ? largestDoc.getFilename() : "N/A")
                .uploadsLast7Days(uploadsLast7Days)
                .uploadsLast30Days(uploadsLast30Days)
                .build();
    }

    /**
     * Get recent activity logs.
     */
    @Transactional(readOnly = true)
    public RecentActivityResponse getRecentActivity(Long userId, Integer limit) {
        int limitValue = limit != null ? limit : 20;

        List<Document> recentDocuments = documentRepository.findByOwnerId(userId).stream()
                .sorted(Comparator.comparing(Document::getUploadTime).reversed())
                .limit(limitValue)
                .collect(Collectors.toList());

        List<RecentActivityResponse.ActivityItem> activities = recentDocuments.stream()
                .map(doc -> {
                    String metadata = buildMetadata(doc);
                    return RecentActivityResponse.ActivityItem.builder()
                            .activityType("UPLOAD")
                            .documentId(doc.getId())
                            .documentName(doc.getFilename())
                            .fileType(getFileTypeFromContentType(doc.getFileType()))
                            .timestamp(doc.getUploadTime())
                            .description("Document uploaded" + 
                                (doc.getVersion() > 1 ? " (version " + doc.getVersion() + ")" : ""))
                            .metadata(metadata)
                            .build();
                })
                .collect(Collectors.toList());

        return RecentActivityResponse.builder()
                .recentUploads(activities)
                .totalItems(activities.size())
                .build();
    }

    /**
     * Build metadata string for activity item.
     */
    private String buildMetadata(Document doc) {
        List<String> metadata = new ArrayList<>();

        if (doc.getExtractedText() != null && !doc.getExtractedText().isBlank()) {
            int wordCount = doc.getExtractedText().split("\\s+").length;
            metadata.add(wordCount + " words");
        }

        long chunkCount = documentChunkRepository.countByDocumentId(doc.getId());
        if (chunkCount > 0) {
            metadata.add(chunkCount + " chunks");
        }

        if (doc.getSummary() != null && !doc.getSummary().isBlank()) {
            metadata.add("AI summary");
        }

        if (doc.getTags() != null && !doc.getTags().isBlank()) {
            int tagCount = doc.getTags().split(",").length;
            metadata.add(tagCount + " tags");
        }

        return metadata.isEmpty() ? "Processing..." : String.join(", ", metadata);
    }

    /**
     * Extract file type from content type.
     */
    private String getFileTypeFromContentType(String contentType) {
        if (contentType == null) {
            return "UNKNOWN";
        }
        if (contentType.contains("pdf")) {
            return "PDF";
        } else if (contentType.contains("wordprocessingml")) {
            return "DOCX";
        } else if (contentType.contains("msword")) {
            return "DOC";
        } else if (contentType.contains("text/plain")) {
            return "TXT";
        }
        return "OTHER";
    }

    /**
     * Format bytes to human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
