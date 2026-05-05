package com.marketplace.backend.dto;

import lombok.Data;

@Data
public class AIDescriptionRequest {
    private String title;
    private String typeLabel;
    private String location;
    private String eventDate;
    private String currentDescription;
}