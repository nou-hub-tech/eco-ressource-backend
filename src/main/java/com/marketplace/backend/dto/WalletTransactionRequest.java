package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class WalletTransactionRequest {

  @NotNull private Long userId;

  @NotBlank private String label;

  @NotBlank private String typeLabel;

  @NotNull private BigDecimal amount;

  private Boolean positiveFlag;

  private String fromParty;

  private String toParty;

  @NotBlank private String status;

  private LocalDate valueDate;
}
