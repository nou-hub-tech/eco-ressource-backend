package com.marketplace.backend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {

  private int status;
  private String error;
  private String message;
  @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();
}
