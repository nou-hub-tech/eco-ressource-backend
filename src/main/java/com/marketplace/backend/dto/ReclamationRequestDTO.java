package com.marketplace.backend.dto;

import
        lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ReclamationRequestDTO {
    private Long stockItemId;
    private Long productId;
    private String description;
    private MultipartFile image;
}