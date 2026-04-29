package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class SolidarityAssociationRequest {

  @NotBlank
  private String name;

  @NotBlank
  private String mission;

  @NotNull
  @PositiveOrZero
  private Integer members;

  @NotNull
  @PositiveOrZero
  private Double donations;

  @NotBlank
  private String statusLabel;

  private String aiInsight;

  /** Optional fundraising goal (maps to goal_amount column). */
  @PositiveOrZero
  private Double goalAmount;
}
