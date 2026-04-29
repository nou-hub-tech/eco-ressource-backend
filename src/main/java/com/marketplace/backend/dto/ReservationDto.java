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
public class ReservationDto {

  private String id;
  private String type;
  private String item;
  private String company;
  private String from;
  private String to;
  private BigDecimal price;
  private String status;
}
