package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransportOfferRequest {

  /**
   * When set (e.g. by admin), offer is created for this transporter; otherwise the caller's
   * transporter profile is used.
   */
  private Long transporterId;

  @NotBlank private String fromLocation;

  @NotBlank private String toLocation;

  @NotBlank private String cargoDescription;

  @NotBlank private String weightLabel;

  @NotNull private BigDecimal proposedEarn;

  /** Optional; defaults to open on create. */
  private String status;
}
