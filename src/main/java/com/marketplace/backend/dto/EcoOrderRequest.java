package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class EcoOrderRequest {

  /** Reference is auto-generated if omitted. */
  private String ref;

  @NotBlank private String companyName;
  @NotBlank private String material;
  @NotNull private BigDecimal qtyKg;
  @NotBlank private String supplier;
  @NotNull @PositiveOrZero private Integer distanceKm;

  private LocalDate orderDate;

  /** Status string — case-insensitive. Defaults to {@code draft} if omitted on create. */
  private String status;

  /** Optional ("A".."E"); inferred from material+distance on create if omitted. */
  private String grade;

  private BigDecimal co2Saved;
  private BigDecimal waterSaved;
  private BigDecimal wasteAvoided;

  private Long enterpriseId;
}
