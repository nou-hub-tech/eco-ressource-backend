package com.marketplace.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolidarityDto {

  private Long id;
  private String name;
  private String mission;
  private int members;
  private int donations;
  private String status;
  private String ai;
}
