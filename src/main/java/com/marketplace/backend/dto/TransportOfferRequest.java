package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransportOfferRequest {

  @NotBlank private String fromLocation;

  @NotBlank private String toLocation;

  @NotBlank private String cargoDescription;

  @NotBlank private String weightLabel;

  @NotNull private BigDecimal proposedEarn;
}
