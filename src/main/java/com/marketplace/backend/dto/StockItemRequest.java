package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockItemRequest {

  @NotNull private Long enterpriseId;

  @NotBlank private String name;

  @NotBlank private String category;

  @NotNull private Integer quantity;

  @NotBlank private String unit;

  private String conditionLabel;

  @NotBlank private String status;

  private String aiInsight;
}
