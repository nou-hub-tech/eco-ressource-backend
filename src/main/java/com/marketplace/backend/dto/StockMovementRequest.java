package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StockMovementRequest {

  private String description;

  private LocalDateTime movementDate;

  private String movementType;

  @NotNull private Integer quantity;

  private Long idStock;
}
