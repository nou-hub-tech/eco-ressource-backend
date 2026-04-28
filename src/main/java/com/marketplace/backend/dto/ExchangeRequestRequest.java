package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ExchangeRequestRequest {

  @NotNull private Long recipientEnterpriseId;

  @NotBlank private String fromCompanyName;

  @NotBlank private String fromAvatar;

  @NotBlank private String item;

  @NotBlank private String typeLabel;

  @NotNull private LocalDate fromDate;

  @NotNull private LocalDate toDate;

  @NotBlank private String durationLabel;

  @NotNull private BigDecimal price;

  @NotBlank private String message;

  @NotBlank private String status;

  @NotBlank private String receivedLabel;

  private boolean urgent;
}
