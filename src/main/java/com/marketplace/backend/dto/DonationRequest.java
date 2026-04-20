package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class DonationRequest {

  @NotNull
  @Positive
  private Double amount;

  private String message;

  @NotNull
  private Long associationId;

  // Optional: if not provided, might be inferred from logged-in user
  private Long userId;
}
