package com.marketplace.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserCreateRequest {

  @NotBlank private String name;

  @NotBlank @Email private String email;

  @NotBlank private String password;

  @NotBlank private String phone;

  /** "enterprise" or "transporter" */
  @NotBlank private String role;

  @NotBlank private String companyName;

  @NotBlank private String sector;

  @NotBlank private String taxId;
}
