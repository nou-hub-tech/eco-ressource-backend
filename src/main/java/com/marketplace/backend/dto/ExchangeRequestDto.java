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
public class ExchangeRequestDto {

  private String id;
  private String from;
  private String avatar;
  private String item;
  private String type;
  private String from_date;
  private String to_date;
  private String duration;
  private BigDecimal price;
  private String message;
  private String status;
  private String received;
  private boolean urgent;
}
