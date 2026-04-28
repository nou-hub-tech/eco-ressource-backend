package com.marketplace.backend.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {

  private String id;
  private String label;
  private String type;
  private BigDecimal amount;
  private Boolean positive;
  private String status;
  private String date;
  private String from;
  private String to;
}
