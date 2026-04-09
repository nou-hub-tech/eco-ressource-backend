package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {

  @NotBlank private String name;

  private String category;

  private String description;

  private String image;

  private String materialType;

  @NotNull private Boolean recyclable;
}
