package com.marketplace.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private String type;
    private Long deliveryOrderId;
    private String clientName;
    private String message;
    private String problemeType;
    private Integer retardMinutes;
    private String transporterName;
    private Instant timestamp;
    private Boolean read;
    private Long targetUserId;
}