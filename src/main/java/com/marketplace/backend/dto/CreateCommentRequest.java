package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {

  @NotBlank(message = "Le contenu du commentaire est obligatoire")
  @Size(min = 1, max = 2000, message = "Le commentaire doit faire entre 1 et 2000 caracteres")
  private String content;

  private Long parentId;
}
