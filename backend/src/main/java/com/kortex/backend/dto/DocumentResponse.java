package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for document response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private Long id;
    private String filename;
    private String fileType;
    private Long size;
    private LocalDateTime uploadTime;
    private Long ownerId;
    private Integer version;
    private Long previousVersionId;
    private Integer pageCount;
    private String contentHash;
    private String summary;
    private LocalDateTime summaryGeneratedAt;
    private String tags;
    private LocalDateTime tagsGeneratedAt;
}
