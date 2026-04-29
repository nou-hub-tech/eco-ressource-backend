package com.marketplace.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ReservationSlotRequest {

  @NotBlank private String machine;

  @NotNull private LocalDate date;

  @NotNull @Min(0) @Max(23) private Integer startHour;

  @NotNull @Min(1) @Max(24) private Integer endHour;

  /** Case-insensitive: open / booked / blocked. Defaults to {@code open}. */
  private String status;

  @NotNull private Boolean solar;

  @Min(0) @Max(100)
  private Integer discountPct;

  @NotBlank private String owner;

  private String reservedBy;

  private Long enterpriseId;
}
