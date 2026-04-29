package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Payload to mark a slot as {@code booked} (drag-&-drop assignment). */
@Data
public class SlotBookRequest {
  @NotBlank
  private String reservedBy;
}
