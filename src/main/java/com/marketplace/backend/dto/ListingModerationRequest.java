package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ListingModerationRequest {

  @NotBlank private String status;
}
