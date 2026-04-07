package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreateListingRequest {

  /** When set (e.g. by admin), listing is created for this enterprise; otherwise the caller's enterprise is used. */
  private Long enterpriseId;

  @NotBlank private String title;

  @NotBlank private String category;

  @NotNull private BigDecimal price;

  @NotBlank private String quantityLabel;

  @NotBlank private String status;

  private String aiInsight;
}
