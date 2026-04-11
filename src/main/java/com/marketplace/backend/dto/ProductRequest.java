package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductRequest {

  @NotBlank(message = "Le nom du produit est obligatoire")
  @Size(min = 2, max = 255, message = "Le nom doit faire entre 2 et 255 caracteres")
  private String name;

  private String category;

  @Size(max = 1000, message = "La description ne doit pas depasser 1000 caracteres")
  private String description;

  private String image;

  private String materialType;

  @NotNull(message = "Le champ recyclable est obligatoire")
  private Boolean recyclable;

  private Long companyId;
}
