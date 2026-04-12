package com.marketplace.backend.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {

  private String fullName;
  private String phone;
  private String city;
  /** active | pending | suspended */
  private String accountStatus;
  private Boolean verified;
  /** When set, replaces password (bcrypt in service). */
  private String password;
}
