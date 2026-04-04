package com.marketplace.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

  private String id;
  private String name;
  private String email;
  /** Angular route role: admin | enterprise | transporter */
  private String role;
  private String company;
  private String avatar;
}
