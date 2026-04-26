package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PlatformEventRequest {

  @NotBlank private String title;

  @NotNull private LocalDate eventDate;

  @NotBlank private String location;

  @NotNull private Double latitude;
  @NotNull private Double longitude;

  @NotNull private Integer participants;

  @NotBlank private String status;

  @NotBlank private String typeLabel;

  private String description;
}
