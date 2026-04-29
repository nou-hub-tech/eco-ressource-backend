package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import lombok.Data;

@Data
public class StockItemRequest {

  private Long companyId;

  private String itemCondition;

  private LocalDate expirationDate;

  private String image;

  private String location;

  @NotNull(message = "La quantite est obligatoire")
  @Positive(message = "La quantite doit etre strictement positive")
  private Integer quantity;

  private String status;

  private String unit;

  private Long idProduct;

  @NotNull(message = "Le prix unitaire est obligatoire")
  @PositiveOrZero(message = "Le prix unitaire doit etre positif ou nul")
  private Double unitPrice;
}
