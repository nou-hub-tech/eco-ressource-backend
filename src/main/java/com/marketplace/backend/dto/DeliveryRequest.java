package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class DeliveryRequest {

  @NotBlank private String productLabel;

  @NotBlank private String fromLocation;

  @NotBlank private String toLocation;

  private String clientName;

  @NotNull private Long enterpriseId;

  private Long transporterId;

  @NotBlank private String status;

  private String co2Label;
  private String dateLabel;
  private String pickupLabel;
  private String deliveryLabel;
  private BigDecimal amount;
  private BigDecimal earnAmount;
}
