package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class StockItemRequest {

  private Long companyId;

  private String itemCondition;

  private LocalDate expirationDate;

  private String image;

  private String location;

  @NotNull private Integer quantity;

  private String status;

  private String unit;

  private Long idProduct;

  @NotNull private Double unitPrice;
}
