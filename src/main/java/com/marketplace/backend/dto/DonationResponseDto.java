package com.marketplace.backend.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Lightweight response DTO for Donation — avoids serializing JPA entity
 * relationships (User, SolidarityAssociation) which cause massive JSON payloads
 * and N+1 Hibernate lazy-load chains.
 */
@Data
@Builder
public class DonationResponseDto {

  private Long id;
  private Double amount;
  private String message;
  private Long associationId;
  private Long userId;      // nullable
  private String userName;  // nullable, to display on frontend
  private LocalDateTime createdAt;
}
