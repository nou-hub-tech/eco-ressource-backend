package com.marketplace.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItemDto {

  private Long id;
  private String name;
  private String category;
  private int qty;
  private String unit;
  private String condition;
  private String status;
  private String ai;
}
