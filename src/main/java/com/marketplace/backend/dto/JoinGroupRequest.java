package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class JoinGroupRequest {

  @NotNull(message = "La quantite est obligatoire")
  @Positive(message = "La quantite doit etre strictement positive")
  private Integer quantity;

  @NotNull(message = "L'identifiant de l'entreprise est obligatoire")
  private Long companyId;
}
