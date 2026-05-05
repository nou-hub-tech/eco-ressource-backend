package com.marketplace.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeocodingResponse {

  private String label;
  private Double latitude;
  private Double longitude;
  private String provider;
}
