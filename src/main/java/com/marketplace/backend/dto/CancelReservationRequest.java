package com.marketplace.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body for soft-deleting a reservation with an audit reason. */
@Data
public class CancelReservationRequest {
  @Size(max = 500)
  private String reason;
}
