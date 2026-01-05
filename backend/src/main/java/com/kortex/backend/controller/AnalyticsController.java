package com.kortex.backend.controller;

import com.kortex.backend.dto.AnalyticsOverviewResponse;
import com.kortex.backend.dto.KeywordFrequencyResponse;
import com.kortex.backend.dto.RecentActivityResponse;
import com.kortex.backend.dto.UploadStatisticsResponse;
import com.kortex.backend.security.UserDetailsImpl;
import com.kortex.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for analytics dashboard.
 * All endpoints are read-only and return statistics about user's documents.
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get overview statistics.
     * 
     * @param authentication the authenticated user
     * @return overview statistics including word count, reading time, storage, etc.
     */
    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("Analytics overview request from user {}", userId);

        AnalyticsOverviewResponse response = analyticsService.getOverviewStatistics(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get keyword frequency statistics.
     * 
     * @param limit maximum number of keywords to return (default: 20)
     * @param authentication the authenticated user
     * @return top keywords with their frequencies
     */
    @GetMapping("/keywords")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KeywordFrequencyResponse> getKeywordFrequency(
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("Keyword frequency request from user {} (limit: {})", userId, limit);

        KeywordFrequencyResponse response = analyticsService.getKeywordFrequency(userId, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get upload statistics.
     * 
     * @param authentication the authenticated user
     * @return upload statistics including time series, file types, sizes
     */
    @GetMapping("/uploads")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UploadStatisticsResponse> getUploadStatistics(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("Upload statistics request from user {}", userId);

        UploadStatisticsResponse response = analyticsService.getUploadStatistics(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get recent activity logs.
     * 
     * @param limit maximum number of activities to return (default: 20)
     * @param authentication the authenticated user
     * @return recent document uploads with metadata
     */
    @GetMapping("/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RecentActivityResponse> getRecentActivity(
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("Recent activity request from user {} (limit: {})", userId, limit);

        RecentActivityResponse response = analyticsService.getRecentActivity(userId, limit);
        return ResponseEntity.ok(response);
    }
}
