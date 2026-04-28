package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ReservationRequest {

  @NotBlank private String typeLabel;

  @NotBlank private String item;

  @NotBlank private String companyName;

  @NotNull private LocalDate fromDate;

  @NotNull private LocalDate toDate;

  @NotNull private BigDecimal price;

  @NotBlank private String status;

  private Long enterpriseId;
}
