package com.marketplace.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CreateListingRequest {

  @NotBlank(message = "Le titre est obligatoire")
  @Size(min = 3, max = 255, message = "Le titre doit faire entre 3 et 255 caracteres")
  private String title;

  @NotBlank(message = "La description est obligatoire")
  @Size(min = 10, max = 2000, message = "La description doit faire entre 10 et 2000 caracteres")
  private String description;

  @NotNull(message = "Le type est obligatoire (SURPLUS, DEMANDE, GROUP_BUYING)")
  private String type;

  @NotNull(message = "La quantite est obligatoire")
  @Positive(message = "La quantite doit etre strictement positive")
  private Integer quantity;

  @NotBlank(message = "L'unite est obligatoire")
  private String unit;

  @DecimalMin(value = "0.0", message = "Le prix doit etre positif ou nul")
  private BigDecimal price;

  private String location;

  @DecimalMin(value = "-90.0", message = "Latitude invalide (min -90)")
  @DecimalMax(value = "90.0", message = "Latitude invalide (max 90)")
  private Double latitude;

  @DecimalMin(value = "-180.0", message = "Longitude invalide (min -180)")
  @DecimalMax(value = "180.0", message = "Longitude invalide (max 180)")
  private Double longitude;

  @NotNull(message = "Le produit est obligatoire")
  private Long productId;

  @NotNull(message = "L'entreprise est obligatoire")
  private Long companyId;

  private List<String> attachmentUrls;

  @Positive(message = "La quantite cible doit etre strictement positive")
  private Integer targetQuantity;

  private LocalDateTime deadline;
}
