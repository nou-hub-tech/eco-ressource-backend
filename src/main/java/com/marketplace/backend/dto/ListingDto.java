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
public class ListingDto {

  private Long id;
  private String title;
  private String company;
  private String category;
  private BigDecimal price;
  private String qty;
  private String status;
  private String ai;
  private Integer views;
  private Integer enquiries;
  private String posted;
  /** Extra fields for enterprise dashboard */
  private String sub;
  private String initials;
  private String priceDisplay;
  private String match;
  private String verified;
  private String rating;
  private String time;
}
