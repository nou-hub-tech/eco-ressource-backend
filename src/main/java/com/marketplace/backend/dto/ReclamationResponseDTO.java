package com.marketplace.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReclamationResponseDTO {
    private Long id;
    private String productName;
    private String description;
    private String defectType;
    private String imageUrl;
    private String status;
    private LocalDateTime createdAt;
    private String aiAnalysis;
    private Double confidence;
    private String recommendations;
}