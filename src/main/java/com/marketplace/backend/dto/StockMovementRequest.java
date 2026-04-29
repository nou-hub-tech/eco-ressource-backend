package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StockMovementRequest {

  private String description;

  private LocalDateTime movementDate;

  private String movementType;

  @NotNull(message = "La quantite est obligatoire")
  @Positive(message = "La quantite doit etre strictement positive")
  private Integer quantity;

  private Long idStock;
}
