package com.marketplace.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body for soft-deleting an order. */
@Data
public class CancelOrderRequest {
  @Size(max = 500)
  private String reason;
}
