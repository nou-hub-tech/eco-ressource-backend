package com.marketplace.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body for soft-deleting a slot. */
@Data
public class CancelSlotRequest {
  @Size(max = 500)
  private String reason;
}
