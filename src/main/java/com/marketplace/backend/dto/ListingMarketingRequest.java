package com.marketplace.backend.dto;

import com.marketplace.backend.entity.enums.ListingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ListingMarketingRequest {

  @NotBlank(message = "Le titre est obligatoire")
  private String title;

  @NotBlank(message = "La description est obligatoire")
  private String description;

  @NotNull(message = "Le type d'annonce est obligatoire")
  private ListingType type;

  @NotNull(message = "La quantite est obligatoire")
  @Positive(message = "La quantite doit etre positive")
  private Integer quantity;

  @NotBlank(message = "L'unite est obligatoire")
  private String unit;

  private String productName;
  private String productCategory;
  private String location;
  private Double price;
}
