package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserStatusRequest {

  @NotBlank private String status;
}
