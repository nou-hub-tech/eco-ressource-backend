package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SolidarityAssociationRequest {

  @NotBlank private String name;

  @NotBlank private String mission;

  @NotNull private Integer members;

  @NotNull private Integer donations;

  @NotBlank private String statusLabel;

  private String aiInsight;
}
