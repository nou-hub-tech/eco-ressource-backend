package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransporterRequest {

  @NotNull private Long userId;

  @NotBlank private String companyName;

  private String sector;
  private String taxId;

  private Integer listingsCount;
  private Integer ordersCount;
  private String revenue;
}
