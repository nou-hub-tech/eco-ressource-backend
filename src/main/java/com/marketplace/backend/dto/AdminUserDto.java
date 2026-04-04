package com.marketplace.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {

  private String id;
  private String name;
  private String email;
  private String company;
  private String role;
  private String status;
  private String phone;
  private String city;
  private String joined;
  private int listings;
  private int orders;
  private String revenue;
  private boolean verified;
  private String avatar;
}
