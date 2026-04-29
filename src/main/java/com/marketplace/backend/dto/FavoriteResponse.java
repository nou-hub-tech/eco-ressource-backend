package com.marketplace.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FavoriteResponse {

  private Long id;
  private Long userId;
  private Long listingId;
  private String listingTitle;
}
