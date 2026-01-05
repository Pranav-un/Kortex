package com.kortex.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for document upload response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploadResponse {

    private Long id;
    private String filename;
    private String fileType;
    private Long size;
    private LocalDateTime uploadTime;
    private String message;
    private Integer version;
    private Long previousVersionId;
}
