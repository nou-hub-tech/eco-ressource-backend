package com.marketplace.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ListingMatchResponse {

  private ListingResponse listing;
  private Integer score;
  private String reason;
}
