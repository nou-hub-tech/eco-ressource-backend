package com.marketplace.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

  private String token;
  private String type;
  /** Spring role e.g. ROLE_ADMIN */
  private String role;
  private UserResponseDto user;
}
